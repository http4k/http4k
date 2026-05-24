package org.http4k.filter.cookie

import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

data class LocalCookie(val cookie: Cookie, val created: Instant, val origin: Uri) {
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
    @Deprecated(message = "This method does not support scope and will return all cookies regardless of origin. " +
        "Use retrieve(uri: Uri) instead." ,
        replaceWith = ReplaceWith("retrieve(uri: Uri)"))
    fun retrieve(): List<LocalCookie>
    @Suppress("DEPRECATION")
    fun retrieve(uri: Uri): List<LocalCookie> = retrieve()
}

@Deprecated(
    message = "BasicCookieStorage has no domain/path/scheme scoping and leaks cookies across origins. " +
        "Use RFC6265CookieStorage instead.",
    replaceWith = ReplaceWith("RFC6265CookieStorage()", "org.http4k.filter.cookie.RFC6265CookieStorage")
)
typealias BasicCookieStorage = InsecureCookieStorage

/**
 * A global cookie jar with no domain, path, or scheme scoping. Cookies stored here are sent to
 * every outgoing request regardless of origin, which can cause cross-origin credential leakage
 * when a single client talks to more than one host.
 *
 * **Use [DefaultCookieStorage] instead.**
 */
class InsecureCookieStorage : CookieStorage {
    private val storage = ConcurrentHashMap<String, LocalCookie>()

    override fun store(cookies: List<LocalCookie>) = cookies.forEach { storage[it.cookie.name] = it }

    @Deprecated(
        "This method does not support scope and will return all cookies regardless of origin. Use retrieve(uri: Uri) instead.",
        replaceWith = ReplaceWith("retrieve(uri: Uri)")
    )
    override fun retrieve(): List<LocalCookie> = storage.values.toList()

    override fun remove(name: String) {
        storage.remove(name)
    }
}

/**
 * An RFC 6265 §5.3/§5.4 compliant cookie storage that scopes cookies by effective domain, path,
 * and scheme (Secure flag). Cookies are only sent to origins that match the domain, path, and
 * scheme under which they were set.
 *
 * Domain matching follows RFC 6265:
 * - Cookies without a `Domain` attribute are host-only and are only sent to the exact host that set them.
 * - Cookies with a `Domain` attribute are sent to that domain and all of its subdomains.
 *
 * Path matching: a stored path of `/foo` matches request paths `/foo`, `/foo/bar`, etc.
 *
 * Secure flag: cookies with `secure = true` are only sent to `https` origins.
 */
class DefaultCookieStorage : CookieStorage {
    private data class Key(val domain: String, val path: String, val name: String, val hostOnly: Boolean)

    private val storage = ConcurrentHashMap<Key, LocalCookie>()

    override fun store(cookies: List<LocalCookie>) {
        for (localCookie in cookies) {
            val cookie = localCookie.cookie
            val originHost = localCookie.origin.host
            val (effectiveDomain, hostOnly) = if (cookie.domain.isNullOrBlank()) {
                originHost to true
            } else {
                cookie.domain.removePrefix(".").lowercase() to false
            }
            val effectivePath = effectivePath(cookie.path, localCookie.origin.path)
            val key = Key(effectiveDomain, effectivePath, cookie.name, hostOnly)
            storage[key] = localCookie
        }
    }

    @Deprecated(
        "This method does not support scope and will return all cookies regardless of origin. Use retrieve(uri: Uri) instead.",
        replaceWith = ReplaceWith("retrieve(uri: Uri)")
    )
    override fun retrieve(): List<LocalCookie> = storage.values.toList()

    override fun retrieve(uri: Uri): List<LocalCookie> {
        val requestHost = uri.host.lowercase()
        val requestPath = uri.path.ifEmpty { "/" }
        val isSecure = uri.scheme.equals("https", ignoreCase = true)

        return storage.values.filter { localCookie ->
            val cookie = localCookie.cookie
            val key = storage.entries.firstOrNull { it.value === localCookie }?.key ?: return@filter false

            if (cookie.secure && !isSecure) return@filter false

            val domainMatch = when {
                key.hostOnly -> requestHost == key.domain
                else -> requestHost == key.domain || requestHost.endsWith(".${key.domain}")
            }

            if (!domainMatch) return@filter false
            pathMatch(requestPath, key.path)
        }
    }

    override fun remove(name: String) {
        storage.keys.removeIf { it.name == name }
    }

    private fun effectivePath(cookiePath: String?, requestPath: String?): String {
        // RFC 6265 §5.1.4 default path
        if (!cookiePath.isNullOrBlank()) return cookiePath
        val path = requestPath ?: "/"
        if (!path.startsWith("/")) return "/"
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash == 0) "/" else path.take(lastSlash)
    }

    // RFC 6265 §5.4 step 2
    private fun pathMatch(requestPath: String, cookiePath: String): Boolean {
        if (requestPath == cookiePath) return true
        if (requestPath.startsWith(cookiePath)) {
            // cookiePath must end with '/' or the next char in requestPath must be '/'
            return cookiePath.endsWith("/") || requestPath[cookiePath.length] == '/'
        }
        return false
    }
}
