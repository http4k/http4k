package org.http4k.filter

import org.http4k.Credentials
import org.http4k.base64Encode
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.filter.ZipkinTraces.Companion.THREAD_LOCAL
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.filter.cookie.CookieStorage
import org.http4k.filter.cookie.LocalCookie
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
                makeRequest(next, request.uri(request.newLocation(location)), attempt + 1)
            } else {
                response
            }
        }

        private fun Request.newLocation(location: String): Uri {
            val locationUri = Uri.of(location)
            return if (locationUri.host.isBlank()) {
                locationUri.authority(uri.authority)
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