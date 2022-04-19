package org.http4k.filter.cookie

import org.http4k.core.cookie.Cookie
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.ConcurrentHashMap

data class LocalCookie(val cookie: Cookie, private val created: Instant) {
    @Deprecated("use main constructor", ReplaceWith("LocalCookie(cookie, created.toInstant(java.time.ZoneOffset.UTC))"))
    constructor(cookie: Cookie, created: LocalDateTime) : this(cookie, created.toInstant(ZoneOffset.UTC))

    @Deprecated("use instant version", ReplaceWith("isExpired(now.toInstant(java.time.ZoneOffset.UTC))"))
    fun isExpired(now: LocalDateTime) = isExpired(now.toInstant(java.time.ZoneOffset.UTC))

    fun isExpired(now: Instant) =
        (cookie.maxAge
            ?.let { maxAge -> Duration.between(created, now).seconds >= maxAge }
            ?: cookie.expires?.let { expires ->
                Duration.between(created, now).seconds > Duration.between(
                    created,
                    expires
                ).seconds
            }) == true
}

interface CookieStorage {
    fun store(cookies: List<LocalCookie>)
    fun remove(name: String)
    fun retrieve(): List<LocalCookie>
}

class BasicCookieStorage : CookieStorage {
    private val storage = ConcurrentHashMap<String, LocalCookie>()

    override fun store(cookies: List<LocalCookie>) = cookies.forEach { storage[it.cookie.name] = it }

    override fun retrieve(): List<LocalCookie> = storage.values.toList()

    override fun remove(name: String) {
        storage.remove(name)
    }
}
