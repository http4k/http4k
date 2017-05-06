package org.reekwest.http.filters

import org.reekwest.http.Credentials
import org.reekwest.http.base64Encode
import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Method
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Uri
import org.reekwest.http.core.cookie.cookie
import org.reekwest.http.core.cookie.cookies
import org.reekwest.http.filters.ZipkinTraces.Companion.THREAD_LOCAL
import org.reekwest.http.filters.cookie.BasicCookieStorage
import org.reekwest.http.filters.cookie.CookieStorage
import org.reekwest.http.filters.cookie.LocalCookie
import java.time.Clock
import java.time.LocalDateTime

object ClientFilters {

    object RequestTracing {
        operator fun invoke(
            startReportFn: (Request, ZipkinTraces) -> Unit = { _, _ -> },
            endReportFn: (Request, Response, ZipkinTraces) -> Unit = { _, _, _ -> }): Filter = Filter {
            next ->
            {
                val traces = THREAD_LOCAL.get()
                startReportFn(it, traces)
                val response = next(ZipkinTraces(traces, it))
                endReportFn(it, response, traces)
                response
            }
        }
    }

    object BasicAuth {
        operator fun invoke(provider: () -> Credentials): Filter = Filter {
            next ->
            { next(it.header("Authorization", "Basic ${provider().base64Encoded()}")) }
        }

        operator fun invoke(user: String, password: String): Filter = BasicAuth(Credentials(user, password))
        operator fun invoke(credentials: Credentials): Filter = BasicAuth({ credentials })

        private fun Credentials.base64Encoded(): String = "$user:$password".base64Encode()
    }

    object FollowRedirects {
        operator fun invoke(): Filter = Filter { next -> { makeRequest(next, it) } }

        private fun makeRequest(next: HttpHandler, request: Request, attempt: Int = 1): Response {
            val response = next(request)
            return if (response.isRedirection() && request.allowsRedirection()) {
                if (attempt == 10) throw IllegalStateException("Too many redirection")
                val location = response.header("location").orEmpty()
                makeRequest(next, request.copy(uri = request.newLocation(location)), attempt + 1)
            } else {
                response
            }
        }

        private fun Request.newLocation(location: String): Uri {
            val locationUri = Uri.uri(location)
            return if (locationUri.host.isBlank()) {
                locationUri.copy(uri.scheme, uri.authority, location)
            } else locationUri
        }

        private fun Response.isRedirection(): Boolean {
            return status.redirection && header("location")?.let(String::isNotBlank) ?: false
        }

        private fun Request.allowsRedirection(): Boolean = method != Method.POST && method != Method.PUT
    }

    object Cookies {
        operator fun invoke(clock: Clock = Clock.systemDefaultZone(),
                            storage: CookieStorage = BasicCookieStorage()): Filter = Filter {
            next ->
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
            .fold(this, { r, cookie -> r.cookie(cookie.name, cookie.value) })

        private fun removeExpired(now: LocalDateTime, storage: CookieStorage)
            = storage.retrieve().filter { it.isExpired(now) }.forEach { storage.remove(it.cookie.name) }

        private fun Clock.now() = LocalDateTime.ofInstant(instant(), zone)
    }
}