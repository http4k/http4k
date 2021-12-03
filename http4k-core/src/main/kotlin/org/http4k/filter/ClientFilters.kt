package org.http4k.filter

import org.http4k.base64Encode
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.ContentType
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.extend
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.GzipCompressionMode.Memory
import org.http4k.filter.ZipkinTraces.Companion.THREAD_LOCAL
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.filter.cookie.CookieStorage
import org.http4k.filter.cookie.LocalCookie
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.RoutingHttpHandler
import org.http4k.security.CredentialsProvider
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

object ClientFilters {

    /**
     * Adds Zipkin request tracing headers to the outbound request. (traceid, spanid, parentspanid)
     */
    fun RequestTracing(
        startReportFn: (Request, ZipkinTraces) -> Unit = { _, _ -> },
        endReportFn: (Request, Response, ZipkinTraces) -> Unit = { _, _, _ -> }
    ): Filter = Filter { next ->
        {
            THREAD_LOCAL.get().run {
                val updated = copy(parentSpanId = spanId, spanId = TraceId.new())
                startReportFn(it, updated)
                val response = next(ZipkinTraces(updated, it))
                endReportFn(it, response, updated)
                response
            }
        }
    }

    /**
     * Reset Zipkin request tracing. Use this to provide a new TraceId for every outbound call.
     */
    fun ResetRequestTracing() = Filter { next ->
        {
            ZipkinTraces.setForCurrentThread(ZipkinTraces(TraceId.new(), TraceId.new(), null))
            next(it)
        }
    }

    /**
     * Sets the host on an outbound request. This is useful to separate configuration of remote endpoints
     * from the logic required to construct the rest of the request.
     */
    fun SetHostFrom(uri: Uri): Filter = Filter { next ->
        {
            next(it.uri(it.uri.scheme(uri.scheme).host(uri.host).port(uri.port))
                .replaceHeader("Host", "${uri.host}${uri.port?.let { port -> ":$port" } ?: ""}"))
        }
    }

    /**
     * Sets the base uri (host + base path) on an outbound request. This is useful to separate configuration of
     * remote endpoints from the logic required to construct the rest of the request.
     */
    fun SetBaseUriFrom(uri: Uri): Filter = SetHostFrom(uri).then(Filter { next ->
        { request -> next(request.uri(uri.extend(request.uri))) }
    })

    /**
     * Copy the Host header into the x-forwarded-host header of a request. Used when we are using proxies
     * to divert traffic to another server.
     */
    fun SetXForwardedHost() = Filter { next ->
        {
            next(it.header("host")
                ?.let { host -> it.replaceHeader("X-Forwarded-Host", host) }
                ?: it
            )
        }
    }

    /**
     * Sets the Content Type request header.
     */
    object SetContentType {
        operator fun invoke(contentType: ContentType): Filter = Filter { next ->
            {
                next(it.with(CONTENT_TYPE of contentType))
            }
        }
    }

    fun ApiKeyAuth(set: (Request) -> Request): Filter = Filter { next ->
        { next(set(it)) }
    }

    object BasicAuth {
        operator fun invoke(provider: () -> Credentials?): Filter = CustomBasicAuth("Authorization", provider)
        operator fun invoke(provider: CredentialsProvider<Credentials>) =
            CustomBasicAuth("Authorization", provider::invoke)

        operator fun invoke(user: String, password: String): Filter = BasicAuth(Credentials(user, password))
        operator fun invoke(credentials: Credentials): Filter = BasicAuth { credentials }
    }

    object ProxyBasicAuth {
        operator fun invoke(provider: () -> Credentials?) = CustomBasicAuth("Proxy-Authorization", provider)
        operator fun invoke(provider: CredentialsProvider<Credentials>) = ProxyBasicAuth(provider::invoke)
        operator fun invoke(user: String, password: String): Filter = ProxyBasicAuth(Credentials(user, password))
        operator fun invoke(credentials: Credentials): Filter = ProxyBasicAuth { credentials }
    }

    object CustomBasicAuth {
        operator fun invoke(header: String, provider: () -> Credentials?): Filter = Filter { next ->
            { req ->
                provider()
                    ?.let {
                        next(req.header(header, "Basic ${it.base64Encoded()}"))
                    }
                    ?: Response(UNAUTHORIZED)
            }
        }

        operator fun invoke(header: String, provider: CredentialsProvider<Credentials>) =
            CustomBasicAuth(header, provider::invoke)

        operator fun invoke(header: String, user: String, password: String): Filter =
            CustomBasicAuth(header, Credentials(user, password))

        operator fun invoke(header: String, credentials: Credentials): Filter = CustomBasicAuth(header) { credentials }

        private fun Credentials.base64Encoded(): String = "$user:$password".base64Encode()
    }

    object BearerAuth {
        operator fun invoke(provider: () -> String?): Filter = Filter { next ->
            { req ->
                provider()
                    ?.let {
                        next(req.header("Authorization", "Bearer $it"))
                    }
                    ?: Response(UNAUTHORIZED)
            }
        }

        operator fun invoke(provider: CredentialsProvider<String>): Filter = BearerAuth(provider::invoke)

        operator fun invoke(token: String): Filter = BearerAuth { token }
    }

    class FollowRedirects : Filter {
        private fun makeRequest(next: HttpHandler, request: Request, attempt: Int = 1): Response =
            next(request).let {
                if (it.isRedirection()) {
                    if (attempt == 10) throw IllegalStateException("Too many redirection")
                    it.assureBodyIsConsumed()
                    if (it.status == SEE_OTHER) {
                        makeRequest(next, request.body(EMPTY).toNewLocation(it.location()), attempt + 1)
                    } else {
                        makeRequest(next, request.toNewLocation(it.location()), attempt + 1)
                    }
                } else it
            }

        private fun Request.toNewLocation(location: String) = ensureValidMethodForRedirect().uri(newLocation(location))

        private fun Response.location() = header("location")?.replace(";\\s*charset=.*$".toRegex(), "").orEmpty()

        private fun Response.assureBodyIsConsumed() = body.close()

        private fun Response.isRedirection(): Boolean =
            status.redirection && header("location")?.let(String::isNotBlank) == true

        private fun Request.ensureValidMethodForRedirect(): Request =
            if (method == GET || method == HEAD) this else method(GET)

        private fun Request.newLocation(location: String): Uri =
            Uri.of(location).run {
                if (host.isBlank()) authority(uri.authority).scheme(uri.scheme) else this
            }

        override fun invoke(next: HttpHandler): HttpHandler = { makeRequest(next, it) }

        /**
         * This filter requires special treatment for routing handlers.
         *
         * In general, Filters are applied _after_ routing (i.e. routing information is available to filters).
         * This filter, however, needs to behave like a browser, and for that we apply the filter _before_ the routing.
         */
        fun then(router: RoutingHttpHandler): HttpHandler = { this(router)(it) }
    }

    object Cookies {
        operator fun invoke(
            clock: Clock = Clock.systemDefaultZone(),
            storage: CookieStorage = BasicCookieStorage()
        ): Filter = Filter { next ->
            { request ->
                val now = clock.now()
                removeExpired(now, storage)
                val response = next(request.withLocalCookies(storage))
                storage.store(response.cookies().map { LocalCookie(it, now) })
                response
            }
        }

        private fun Request.withLocalCookies(storage: CookieStorage) = storage.retrieve()
            .map { it.cookie }
            .fold(this) { r, cookie -> r.cookie(cookie.name, cookie.value) }

        private fun removeExpired(now: LocalDateTime, storage: CookieStorage) =
            storage.retrieve().filter { it.isExpired(now) }.forEach { storage.remove(it.cookie.name) }

        private fun Clock.now() = LocalDateTime.ofInstant(instant(), ZoneOffset.UTC)
    }

    /**
     * Support for GZipped responses from clients.
     */
    fun AcceptGZip(compressionMode: GzipCompressionMode = Memory): Filter =
        ResponseFilters.GunZip(compressionMode)

    /**
     * Basic GZip and Gunzip support of Request/Response.
     * Only Gunzip responses when the response contains "transfer-encoding" header containing 'gzip'
     */
    fun GZip(compressionMode: GzipCompressionMode = Memory): Filter =
        RequestFilters.GZip(compressionMode)
            .then(ResponseFilters.GunZip(compressionMode))

    /**
     * This Filter is used to clean the Request and Response when proxying directly to another system. The purpose
     * of this is to remove any routing metadata that we may have attached to it before sending it onwards.
     */
    fun CleanProxy() = Filter { next ->
        {
            next(it.run { Request(method, uri).body(body).headers(headers) }).run {
                Response(status).body(body).headers(headers)
            }
        }
    }
}
