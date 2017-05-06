package org.reekwest.http.core.cookie

import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import java.time.Clock
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

data class LocalCookie(val cookie: Cookie, val created: LocalDateTime) {
    fun isExpired(now: LocalDateTime) =
        cookie.maxAge?.let { maxAge -> ChronoUnit.SECONDS.between(created, now).dec() >= maxAge }
            ?: cookie.expires?.let { expires -> ChronoUnit.SECONDS.between(created, now).dec() > ChronoUnit.SECONDS.between(created, expires).dec() }
            ?: false
}

interface CookieStorage {
    fun store(cookies: List<LocalCookie>)
    fun remove(name: String)
    fun retrieve(): List<LocalCookie>
}

class ClientCookies(private val clock: Clock = Clock.systemDefaultZone(), private val storage: CookieStorage = BasicCookieStorage()) : Filter {
    override fun invoke(handler: HttpHandler): HttpHandler = { request ->
        val now = clock.now()
        removeExpired(now)
        val response = handler(request.withLocalCookies())
        storage.store(response.cookies().map { LocalCookie(it, now) })
        response
    }

    private fun Request.withLocalCookies(): Request = storage.retrieve()
        .map { it.cookie }
        .fold(this, { r, cookie -> r.cookie(cookie.name, cookie.value) })

    private fun removeExpired(now: LocalDateTime) {
        storage.retrieve().filter { it.isExpired(now) }.forEach { storage.remove(it.cookie.name) }
    }

    private fun Clock.now() = LocalDateTime.ofInstant(instant(), zone)
}

class BasicCookieStorage : CookieStorage {
    val storage = ConcurrentHashMap<String, LocalCookie>()

    override fun store(cookies: List<LocalCookie>) = cookies.forEach { storage.put(it.cookie.name, it) }

    override fun retrieve(): List<LocalCookie> = storage.values.toList()

    override fun remove(name: String) {
        storage.remove(name)
    }
}
