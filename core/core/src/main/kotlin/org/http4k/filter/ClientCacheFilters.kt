package org.http4k.filter

import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.Filter
import org.http4k.core.Headers
import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.MemoryResponse
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.TRACE
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_MODIFIED
import org.http4k.core.Uri
import org.http4k.core.relative
import org.http4k.length
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

/**
 * A single cached response for a single origin URI, along with the metadata required to make
 * browser-like caching decisions (freshness, revalidation, and Vary matching) against it.
 *
 * The stored [response] is always a [MemoryResponse] - i.e. any streamed body has been realized -
 * so that it can be replayed to many callers without worrying about consuming an underlying stream.
 */
data class CachedResponse(
    val origin: Uri,
    val method: Method,
    val requestHeaders: Headers,
    val response: Response,
    val receivedAt: Instant
)

/**
 * Storage abstraction for the responses cached by [ClientCacheFilters]. Mirrors the design of
 * [org.http4k.filter.cookie.CookieStorage] allows custom (e.g. disk-backed) implementations to
 * be plugged in.
 */
interface ClientCacheStorage {
    fun store(uri: Uri, cached: CachedResponse)
    fun retrieve(uri: Uri): List<CachedResponse> = emptyList()
    fun remove(uri: Uri)
    fun clear()
}

/**
 * Default thread-safe, last-writer-wins, bounded in-memory cache with a simple LRU eviction policy.
 */
class InMemoryClientCacheStorage(private val maxEntries: Int = 1000) : ClientCacheStorage {

    private val storage = object : LinkedHashMap<Key, CachedResponse>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Key, CachedResponse>): Boolean = size > maxEntries
    }

    @Synchronized
    override fun store(uri: Uri, cached: CachedResponse) {
        storage.put(Key(uri, cached.method), cached)
    }

    @Synchronized
    override fun retrieve(uri: Uri): List<CachedResponse> {
        val keys = storage.keys.filter { it.uri == uri }
        return keys.mapNotNull { storage[it] }
    }

    @Synchronized
    override fun remove(uri: Uri) {
        storage.keys.removeIf { it.uri == uri }
    }

    @Synchronized
    override fun clear() = storage.clear()

    private data class Key(val uri: Uri, val method: Method)
}

/**
 * Browser-like caching filter for HTTP clients.
 *
 * Wrapped around any [HttpHandler] (typically a client), this filter implements client-side
 * caching following [RFC 9111](https://www.rfc-editor.org/rfc/rfc9111.html):
 *
 * - Fresh responses (via `Cache-Control: max-age`/`s-maxage`, `Expires`, or heuristic caching)
 *   are served straight from the cache without touching the network.
 * - Stale responses are revalidated using the cached `ETag` (`If-None-Match`) and/or
 *   `Last-Modified` (`If-Modified-Since`) validators; a `304 Not Modified` is translated back
 *   into the cached representation for the caller.
 * - `Vary` is honoured, request `Cache-Control` directives (`no-store`, `no-cache`,
 *   `only-if-cached`, `max-age`, `max-stale`, `min-fresh`) are respected, and write methods
 *   invalidate the cached entry for the target URI (and its `Location`/`Content-Location`).
 *
 * A single cached variant is kept per method+URI (last-writer-wins across `Vary` variants) and
 * responses containing `Set-Cookie` are still cached, as a browser would. This filter is
 * stateless, so it is safe to share a single instance across threads.
 */
object ClientCacheFilters {

    /**
     * Status codes that are cacheable by default, as per RFC 9111 §3.5.
     */
    val DEFAULT_CACHEABLE_STATUS_CODES: Set<Int> = setOf(200, 203, 204, 206, 300, 301, 308, 404, 405, 410, 414, 501)

    operator fun invoke(
        storage: ClientCacheStorage = InMemoryClientCacheStorage(),
        timeSource: () -> Instant = Clock.systemUTC()::instant,
        heuristicCaching: Boolean = true,
        shared: Boolean = false,
        cacheableStatusCodes: Set<Int> = DEFAULT_CACHEABLE_STATUS_CODES
    ): Filter = Filter { next ->
        val cache = CacheExecutor(storage, timeSource, heuristicCaching, shared, cacheableStatusCodes)
        return@Filter { request -> cache.execute(next, request) }
    }

    private class CacheExecutor(
        private val storage: ClientCacheStorage,
        private val timeSource: () -> Instant,
        private val heuristicCaching: Boolean,
        private val shared: Boolean,
        private val cacheableStatusCodes: Set<Int>
    ) {

        fun execute(next: HttpHandler, request: Request): Response {
            if (!request.method.isSafe()) return unsafe(next, request)
            if (request.directives().noStore) return next(request)
            val now = timeSource()
            val cached = storage.retrieve(request.uri).firstOrNull { it.matches(request) }
            return if (cached == null) onMiss(next, request, now) else decideAndServe(cached, request, next, now)
        }

        private fun unsafe(next: HttpHandler, request: Request): Response {
            val response = next(request)
            invalidate(request.uri, response)
            return response
        }

        private fun onMiss(next: HttpHandler, request: Request, now: Instant): Response {
            if (request.directives().onlyIfCached) return onlyIfCachedUnavailable()
            val response = next(request)
            return if (shouldStore(request, response)) {
                val memory = response.toMemoryResponse()
                storage.store(request.uri, toCached(request, memory, now))
                memory
            } else {
                response
            }
        }

        private fun decideAndServe(cached: CachedResponse, request: Request, next: HttpHandler, now: Instant): Response {
            val req = request.directives()
            val res = cached.response.directives()
            val lifetime = cached.freshnessLifetime()
            val age = cached.currentAge(now)
            val staleFor = lifetime?.let { age - it }

            if (req.onlyIfCached) return cached.replay(request, now)
            if (isFreshlyServable(req, res, lifetime, age)) return cached.replay(request, now)
            if (isStalelyServable(req, res, staleFor)) return cached.replay(request, now)
            return revalidate(cached, request, next, now)
        }

        private fun revalidate(cached: CachedResponse, request: Request, next: HttpHandler, now: Instant): Response {
            val conditional = request
                .header("If-None-Match", cached.response.header("ETag"))
                .header("If-Modified-Since", cached.response.header("Last-Modified"))
            val response = next(conditional)
            return if (response.status == NOT_MODIFIED && cached.varyMatches(request)) {
                val updated = toCached(request, cached.mergedWith(response), now)
                storage.store(request.uri, updated)
                updated.replay(request, now)
            } else if (response.status.code >= 500 && cached.servableWhenError(now, request)) {
                cached.replay(request, now)
            } else if (shouldStore(request, response)) {
                val memory = response.toMemoryResponse()
                storage.store(request.uri, toCached(request, memory, now))
                memory
            } else {
                response
            }
        }

        private fun isFreshlyServable(req: CacheDirectives, res: CacheDirectives, lifetime: Duration?, age: Duration): Boolean {
            if (lifetime == null) return false
            if (age >= lifetime) return false
            if (res.immutable) return true
            return !requestForcesRevalidation(req, age, lifetime)
        }

        private fun requestForcesRevalidation(req: CacheDirectives, age: Duration, lifetime: Duration): Boolean {
            val maxAgeExceeded = req.maxAge?.let { age > it } ?: false
            val minFreshMissed = req.minFresh?.let { lifetime - age < it } ?: false
            return req.noCache || maxAgeExceeded || minFreshMissed
        }

        private fun isStalelyServable(req: CacheDirectives, res: CacheDirectives, staleFor: Duration?): Boolean {
            if (res.noCache) return false
            if (res.mustRevalidate || res.proxyRevalidate) return false
            if (withinStaleWhileRevalidate(res, staleFor)) return true
            if (!req.maxStalePresent) return false
            return maxStaleAllows(req, staleFor)
        }

        private fun withinStaleWhileRevalidate(res: CacheDirectives, staleFor: Duration?): Boolean {
            val swr = res.staleWhileRevalidate ?: return false
            if (staleFor == null) return false
            return staleFor >= Duration.ZERO && staleFor <= swr
        }

        private fun maxStaleAllows(req: CacheDirectives, staleFor: Duration?): Boolean {
            if (staleFor == null) return false
            val maxStale = req.maxStale
            if (maxStale == null) return true
            return staleFor <= maxStale
        }

        private fun invalidate(uri: Uri, response: Response) {
            storage.remove(uri)
            response.header("Location")?.takeIf(String::isNotBlank)?.let { storage.remove(uri.relative(it)) }
            response.header("Content-Location")?.takeIf(String::isNotBlank)?.let { storage.remove(uri.relative(it)) }
        }

        private fun CachedResponse.mergedWith(notModified: Response): Response {
            val updated = notModified.headers.fold(response) { memo, (name, value) ->
                when (name.lowercase()) {
                    "content-length", "content-type", "content-encoding" -> memo
                    else -> memo.replaceHeader(name, value)
                }
            }.removeHeader("Warning")
            return updated
                .removeHeader("Content-Length")
                .replaceHeader("Content-Length", response.body.length.toString())
        }

        private fun CachedResponse.matches(request: Request): Boolean {
            val methodMatches = method == request.method || (method == GET && request.method == HEAD)
            return methodMatches && varyMatches(request)
        }

        private fun CachedResponse.varyMatches(request: Request): Boolean {
            val vary = response.header("Vary") ?: return true
            if (vary.trim() == "*") return false
            val names = vary.split(",").map(String::trim).filter(String::isNotEmpty)
            if (names.isEmpty()) return true
            return names.all { name ->
                request.headerValues(name).normalizedValues() == requestHeaders.valuesFor(name).normalizedValues()
            }
        }

        private fun CachedResponse.currentAge(now: Instant): Duration {
            val headerAge = response.header("Age")?.toLongOrNull()
            val base = if (headerAge == null) Duration.ZERO else Duration.ofSeconds(headerAge)
            return base.plus(Duration.between(receivedAt, now).coerceAtLeast(Duration.ZERO))
        }

        private fun CachedResponse.freshnessLifetime(): Duration? {
            val res = response.directives()
            if (res.noCache) return Duration.ZERO
            val maxAge = if (shared) res.sMaxAge ?: res.maxAge else res.maxAge
            if (maxAge != null) return maxAge
            val expires = response.header("Expires")?.rfc1123()
            if (expires != null) {
                val date = response.header("Date")?.rfc1123() ?: receivedAt
                return Duration.between(date, expires)
            }
            return heuristicLifetime()
        }

        private fun CachedResponse.heuristicLifetime(): Duration? {
            if (!heuristicCaching) return null
            val lastModified = response.header("Last-Modified")?.rfc1123()
            if (lastModified == null) return null
            val date = response.header("Date")?.rfc1123() ?: receivedAt
            if (!date.isAfter(lastModified)) return null
            return Duration.between(lastModified, date).dividedBy(10)
        }

        private fun CachedResponse.servableWhenError(now: Instant, request: Request): Boolean {
            val res = response.directives()
            if (res.forbidsStaleOnError()) return false
            val lifetime = freshnessLifetime() ?: return false
            val staleFor = currentAge(now) - lifetime
            if (res.staleIfError?.let { staleFor <= it } == true) return true
            return maxStaleAllows(request.directives(), staleFor)
        }

        private fun CacheDirectives.forbidsStaleOnError(): Boolean =
            noCache || noStore || mustRevalidate || proxyRevalidate

        private fun CachedResponse.replay(request: Request, now: Instant): Response {
            var replayed = response.replaceHeader("Age", currentAge(now).seconds.coerceAtLeast(0).toString())
            if (request.method == HEAD) replayed = replayed.body(EMPTY)
            return replayed
        }

        private fun shouldStore(request: Request, response: Response): Boolean {
            if (storeDisallowedBy(request, response)) return false
            val res = response.directives()
            val authorization = request.header("Authorization")
            if (authorization == null) return true
            return allowsAuthorization(res)
        }

        private fun storeDisallowedBy(request: Request, response: Response): Boolean {
            val res = response.directives()
            if (request.method != GET) return true
            if (res.noStore) return true
            if (response.status.code !in cacheableStatusCodes) return true
            return response.header("Vary")?.trim() == "*"
        }

        private fun allowsAuthorization(res: CacheDirectives): Boolean =
            res.public || res.sMaxAge != null || res.mustRevalidate

        private fun toCached(request: Request, response: Response, receivedAt: Instant) = CachedResponse(
            origin = request.uri,
            method = request.method,
            requestHeaders = request.headers,
            response = response.toMemoryResponse(),
            receivedAt = receivedAt
        )

        private fun Response.toMemoryResponse(): Response {
            val buffer = body.payload.duplicate()
            val bytes = ByteArray(buffer.length()).also { buffer.get(it) }
            return MemoryResponse(status, headers, MemoryBody(bytes), version)
        }

        private fun onlyIfCachedUnavailable() = Response(GATEWAY_TIMEOUT)
            .header("Cache-Control", "no-cache")
            .body("only-if-cached requested but the response is not in the cache")
    }

    private fun Method.isSafe() = this == GET || this == HEAD || this == OPTIONS || this == TRACE

    private fun Request.directives() = CacheDirectives.of(header("Cache-Control"))

    private fun Response.directives() = CacheDirectives.of(header("Cache-Control"))

    private fun String?.rfc1123(): Instant? =
        this?.let { runCatching { ZonedDateTime.parse(it, RFC_1123_DATE_TIME).toInstant() }.getOrNull() }

    private fun List<String?>.normalizedValues() = map { it?.trim() ?: "" }.sorted()

    private fun Headers.valuesFor(name: String): List<String?> =
        filter { it.first.equals(name, ignoreCase = true) }.map { it.second }
}

/**
 * Parsed representation of a `Cache-Control` header value (RFC 9111 §5.2).
 */
private data class CacheDirectives(
    val maxAge: Duration? = null,
    val sMaxAge: Duration? = null,
    val minFresh: Duration? = null,
    val maxStale: Duration? = null,
    val maxStalePresent: Boolean = false,
    val staleWhileRevalidate: Duration? = null,
    val staleIfError: Duration? = null,
    val noCache: Boolean = false,
    val noStore: Boolean = false,
    val mustRevalidate: Boolean = false,
    val proxyRevalidate: Boolean = false,
    val public: Boolean = false,
    val private: Boolean = false,
    val immutable: Boolean = false,
    val onlyIfCached: Boolean = false
) {
    companion object {
        fun of(header: String?): CacheDirectives {
            val directives = parse(header)
            return CacheDirectives(
                maxAge = directives.secondsFor("max-age"),
                sMaxAge = directives.secondsFor("s-maxage"),
                minFresh = directives.secondsFor("min-fresh"),
                maxStale = directives.secondsFor("max-stale"),
                maxStalePresent = directives.containsKey("max-stale"),
                staleWhileRevalidate = directives.secondsFor("stale-while-revalidate"),
                staleIfError = directives.secondsFor("stale-if-error"),
                noCache = directives.containsKey("no-cache"),
                noStore = directives.containsKey("no-store"),
                mustRevalidate = directives.containsKey("must-revalidate"),
                proxyRevalidate = directives.containsKey("proxy-revalidate"),
                public = directives.containsKey("public"),
                private = directives.containsKey("private"),
                immutable = directives.containsKey("immutable"),
                onlyIfCached = directives.containsKey("only-if-cached")
            )
        }

        private fun parse(header: String?): Map<String, String?> {
            val parsed = mutableMapOf<String, String?>()
            header?.split(",")?.forEach { token ->
                val name = token.substringBefore('=').trim().lowercase()
                if (name.isEmpty()) return@forEach
                val value = token.substringAfter('=', "").trim()
                parsed[name] = value.ifEmpty { null }
            }
            return parsed
        }

        private fun Map<String, String?>.secondsFor(name: String): Duration? =
            this[name]?.toLongOrNull()?.let(Duration::ofSeconds)
    }
}
