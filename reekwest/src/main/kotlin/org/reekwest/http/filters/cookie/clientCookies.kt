package org.reekwest.http.filters.cookie

import org.reekwest.http.core.cookie.Cookie
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

class BasicCookieStorage : CookieStorage {
    val storage = ConcurrentHashMap<String, LocalCookie>()

    override fun store(cookies: List<LocalCookie>) = cookies.forEach { storage.put(it.cookie.name, it) }

    override fun retrieve(): List<LocalCookie> = storage.values.toList()

    override fun remove(name: String) {
        storage.remove(name)
    }
}