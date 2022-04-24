package org.http4k.filter

import org.http4k.base64Decoded
import org.http4k.core.ContentType
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.RequestContext
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Status.Companion.UNSUPPORTED_MEDIA_TYPE
import org.http4k.core.Store
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.GzipCompressionMode.Memory
import org.http4k.filter.ZipkinTraces.Companion.THREAD_LOCAL
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Lens
import org.http4k.lens.LensFailure
import org.http4k.lens.RequestContextLens
import org.http4k.routing.ResourceLoader
import org.http4k.routing.ResourceLoader.Companion.Classpath
import java.io.PrintWriter
import java.io.StringWriter

data class CorsPolicy(
    val originPolicy: OriginPolicy,
    val headers: List<String>,
    val methods: List<Method>,
    val credentials: Boolean = false
) {
    companion object {
        val UnsafeGlobalPermissive =
            CorsPolicy(OriginPolicy.AllowAll(), listOf("content-type"), Method.values().toList(), true)
    }
}

object ServerFilters {

    /**
     * Add Cors headers to the Response, according to the passed CorsPolicy
     */
    object Cors {
        private fun List<String>.joined() = joinToString(", ")

        operator fun invoke(policy: CorsPolicy) = Filter { next ->
            {
                val response = if (it.method == OPTIONS) Response(OK) else next(it)

                val origin = it.header("Origin")
                val allowedOrigin = when {
                    policy.originPolicy is AllowAllOriginPolicy -> "*"
                    origin != null && policy.originPolicy(origin) -> origin
                    else -> "null"
                }

                response.with(
                    Header.required("access-control-allow-origin") of allowedOrigin,
                    Header.required("access-control-allow-headers") of policy.headers.joined(),
                    Header.required("access-control-allow-methods") of policy.methods.map { method -> method.name }
                        .joined(),
                    { res -> if (policy.credentials) res.header("access-control-allow-credentials", "true") else res }
                )
            }
        }
    }

    /**
     * Adds Zipkin request tracing headers to the incoming request and outbound response. (traceid, spanid, parentspanid)
     */
    object RequestTracing {
        operator fun invoke(
            startReportFn: (Request, ZipkinTraces) -> Unit = { _, _ -> },
            endReportFn: (Request, Response, ZipkinTraces) -> Unit = { _, _, _ -> }
        ): Filter = Filter { next ->
            {
                val previous = THREAD_LOCAL.get()

                val fromRequest = ZipkinTraces(it)
                startReportFn(it, fromRequest)
                THREAD_LOCAL.set(fromRequest)

                try {
                    val response = ZipkinTraces(fromRequest, next(ZipkinTraces(fromRequest, it)))
                    endReportFn(it, response, fromRequest)
                    response
                } finally {
                    THREAD_LOCAL.set(previous)
                }
            }
        }
    }

    /**
     * Simple Basic Auth credential checking.
     */
    object BasicAuth {
        /**
         * Credentials validation function
         */
        operator fun invoke(realm: String, authorize: (Credentials) -> Boolean) = Filter { next ->
            {
                val credentials = it.basicAuthenticationCredentials()
                if (credentials == null || !authorize(credentials)) {
                    Response(UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
                } else next(it)
            }
        }

        /**
         * Static username/password validation
         */
        operator fun invoke(realm: String, user: String, password: String) = this(realm, Credentials(user, password))

        /**
         * Static credentials validation
         */
        operator fun invoke(realm: String, credentials: Credentials) = this(realm) { it == credentials }

        /**
         * Population of a RequestContext with custom principal object
         */
        operator fun <T> invoke(realm: String, key: RequestContextLens<T>, lookup: (Credentials) -> T?) =
            Filter { next ->
                {
                    it.basicAuthenticationCredentials()
                        ?.let(lookup)
                        ?.let { found -> next(it.with(key of found)) }
                        ?: Response(UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
                }
            }

        private fun Request.basicAuthenticationCredentials(): Credentials? = header("Authorization")
            ?.trim()
            ?.takeIf { it.startsWith("Basic") }
            ?.substringAfter("Basic")
            ?.trim()
            ?.safeBase64Decoded()
            ?.toCredentials()

        private fun String.safeBase64Decoded(): String? = try {
            base64Decoded()
        } catch (e: IllegalArgumentException) { null }

        private fun String.toCredentials(): Credentials =
            split(":", ignoreCase = false, limit = 2)
            .let { Credentials(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }) }
    }

    /**
     * Bearer Auth token checking.
     */
    object BearerAuth {
        /**
         * Static token validation
         */
        operator fun invoke(token: String) = BearerAuth { it == token }

        /**
         * Static token validation function
         */
        operator fun invoke(checkToken: (String) -> Boolean) = Filter { next ->
            {
                if (it.bearerToken()?.let(checkToken) == true) next(it) else Response(UNAUTHORIZED)
            }
        }

        /**
         * Population of a RequestContext with custom principal object
         */
        operator fun <T> invoke(key: RequestContextLens<T>, lookup: (String) -> T?) = Filter { next ->
            {
                it.bearerToken()
                    ?.let(lookup)
                    ?.let { found -> next(it.with(key of found)) }
                    ?: Response(UNAUTHORIZED)
            }
        }

        private fun Request.bearerToken(): String? = header("Authorization")
            ?.trim()
            ?.takeIf { it.startsWith("Bearer") }
            ?.substringAfter("Bearer")
            ?.trim()
    }

    /**
     * ApiKey token checking.
     */
    object ApiKeyAuth {
        /**
         * ApiKey token checking using a typed lens.
         */
        operator fun <T> invoke(
            lens: (Lens<Request, T>),
            validate: (T) -> Boolean
        ) = ApiKeyAuth { req: Request ->
            try {
                validate(lens(req))
            } catch (e: LensFailure) {
                false
            }
        }

        /**
         * ApiKey token checking using standard request inspection.
         */
        operator fun invoke(validate: (Request) -> Boolean): Filter = Filter { next ->
            {
                when {
                    validate(it) -> next(it)
                    else -> Response(UNAUTHORIZED)
                }
            }
        }
    }

    /**
     * Converts Lens extraction failures into correct HTTP responses (Bad Requests/UnsupportedMediaType).
     * This is required when using lenses to automatically unmarshall inbound requests.
     * Note that LensFailures from unmarshalling upstream Response objects are NOT caught to avoid incorrect server behaviour.
     */
    object CatchLensFailure : Filter by CatchLensFailure()

    /**
     * Converts Lens extraction failures into correct HTTP responses (Bad Requests/UnsupportedMediaType).
     * This is required when using lenses to automatically unmarshall inbound requests.
     * Note that LensFailures from unmarshalling upstream Response objects are NOT caught to avoid incorrect server behaviour.
     *
     * Pass the failResponseFn param to provide a custom response for the LensFailure case
     */
    fun CatchLensFailure(
        failResponseFn: (LensFailure) -> Response = { Response(BAD_REQUEST.description(it.failures.joinToString("; "))) }
    ) = Filter { next ->
        {
            try {
                next(it)
            } catch (lensFailure: LensFailure) {
                when {
                    lensFailure.target is Response -> throw lensFailure
                    lensFailure.target is RequestContext -> throw lensFailure
                    lensFailure.overall() == Failure.Type.Unsupported -> Response(UNSUPPORTED_MEDIA_TYPE)
                    else -> failResponseFn(lensFailure)
                }
            }
        }
    }

    /**
     * Last gasp filter which catches all `Throwable`s and invokes `onError`.
     * The default `onError` is backward compatible with previous implementations,
     * returning INTERNAL_SERVER_ERROR and a formatted stack trace for `Exception`s,
     * and leaking other `Throwable`s.
     *
     * We suggest that you override this behaviour in public-facing systems to log the
     * stack trace rather than show it to the world.
     */
    object CatchAll {
        operator fun invoke(
            onError: (Throwable) -> Response = ::originalBehaviour,
        ): Filter = Filter { next ->
            {
                try {
                    next(it)
                } catch (e: Throwable) {
                    onError(e)
                }
            }
        }

        fun originalBehaviour(e: Throwable): Response {
            if (e !is Exception) throw e
            val stackTraceAsString = StringWriter().apply {
                e.printStackTrace(PrintWriter(this))
            }.toString()
            return Response(INTERNAL_SERVER_ERROR).body(stackTraceAsString)
        }
    }

    /**
     * Copy headers from the incoming request to the outbound response.
     */
    object CopyHeaders {
        operator fun invoke(vararg headers: String): Filter = Filter { next ->
            { request ->
                headers.fold(next(request)) { memo, name ->
                    request.header(name)?.let { memo.header(name, it) } ?: memo
                }
            }
        }
    }

    /**
     * Basic GZip and Gunzip support of Request/Response.
     * Only Gunzips requests which contain "transfer-encoding" header containing 'gzip'
     * Only Gzips responses when request contains "accept-encoding" header containing 'gzip'.
     */
    object GZip {
        operator fun invoke(compressionMode: GzipCompressionMode = Memory): Filter =
            RequestFilters.GunZip(compressionMode).then(ResponseFilters.GZip(compressionMode))
    }

    /**
     * Basic GZip and Gunzip support of Request/Response where the content-type is in the allowed list.
     * Only Gunzips requests which contain "transfer-encoding" header containing 'gzip'
     * Only Gzips responses when request contains "accept-encoding" header containing 'gzip' and the content-type (sans-charset) is one of the compressible types.
     */
    class GZipContentTypes(
        private val compressibleContentTypes: Set<ContentType>,
        private val compressionMode: GzipCompressionMode = Memory
    ) : Filter {
        override fun invoke(next: HttpHandler) = RequestFilters.GunZip(compressionMode)
            .then(ResponseFilters.GZipContentTypes(compressibleContentTypes, compressionMode))
            .invoke(next)
    }

    /**
     * Initialise a RequestContext for each request which passes through the Filter stack,
     */
    object InitialiseRequestContext {
        operator fun invoke(contexts: Store<RequestContext>): Filter = Filter { next ->
            {
                val context = RequestContext()
                try {
                    next(contexts.inject(context, it))
                } finally {
                    contexts.remove(context)
                }
            }
        }
    }

    /**
     * Sets the Content Type response header on the Response.
     */
    object SetContentType {
        operator fun invoke(contentType: ContentType): Filter = Filter { next ->
            {
                next(it).with(CONTENT_TYPE of contentType)
            }
        }
    }

    /**
     * Sets the Content-Disposition response header on the Response for the selected path extensions.
     * By default all extensions are selected, including paths with no extension.
     * If no path is present, the filename will be set to unnamed.
     */
    object ContentDispositionAttachment {
        private fun Request.extension(): String = this.uri.path.substringAfterLast(".", "")
        private fun Request.filename() =
            this.uri.path.split("/").last().let { if(it.isBlank()) "unnamed" else it }
        private val ALL_EXTENSIONS = setOf("*")

        operator fun invoke(extensions: Set<String> = ALL_EXTENSIONS): Filter = Filter { next ->
            { request ->
                next(request).let { response ->
                    val extensionSelected = extensions == ALL_EXTENSIONS || extensions.contains(request.extension())
                    when {
                        response.status.successful && extensionSelected ->
                            response.header(
                                "Content-Disposition",
                                "attachment; filename=${request.filename()}"
                            )
                        else -> response
                    }
                }
            }
        }
    }

    /**
     * Intercepts responses and replaces the contents with contents of the statically loaded resource.
     * By default, this Filter replaces the contents of unsuccessful requests with the contents of a file named
     * after the status code.
     */
    object ReplaceResponseContentsWithStaticFile {
        operator fun invoke(
            loader: ResourceLoader = Classpath(),
            toResourceName: (Response) -> String? = { if (it.status.successful) null else it.status.code.toString() }
        ): Filter = Filter { next ->
            {
                val response = next(it)
                toResourceName(response)
                    ?.let {
                        response.body(loader.load(it)?.readText() ?: "")
                    } ?: response
            }
        }
    }
}
