# Client-side HTTP caching with `ClientCacheFilters`

The caching logic in [`ClientCacheFilters.kt`](./ClientCacheFilters.kt) follows
[RFC 9111 "HTTP Caching"](https://www.rfc-editor.org/rfc/rfc9111.html).

## The problem

HTTP clients typically make a network round-trip for every request, even when the server has
declared the response to be cacheable. That wastes bandwidth, adds latency, and generates
unnecessary load on the origin server. Browsers avoid this with a caching layer that:

1. serves fresh responses straight from a local cache without touching the network;
2. revalidates stale responses against the origin using entity validators;
3. turns a `304 Not Modified` response back into the cached representation.

`ClientCacheFilters` brings that behaviour to any http4k `HttpHandler` (typically a client) as a
single, stateless `Filter`:

```kotlin
val client = ClientCacheFilters().then(JavaHttpClient())
```

The filter reads the storage/expiry instructions from the server's response headers
(`Cache-Control`, `Expires`, `Age`, `ETag`, `Last-Modified`, `Vary`) and from the request's own
`Cache-Control` directives, and automatically fills the cache as traffic flows through it. The
storage layer is pluggable via `ClientCacheStorage`; the default
[`InMemoryClientCacheStorage`](./ClientCacheFilters.kt) is a bounded, thread-safe, LRU cache.

## How requests are decided

For every `GET`/`HEAD` request the filter computes:

- **Freshness lifetime** - how long the response may be considered fresh
  (`max-age`/`s-maxage`, `Expires`, or heuristic).
- **Current age** - `Age` header + time spent in this cache.
- **Whether it can serve stale** - subject to `stale-while-revalidate`, `stale-if-error`,
  `max-stale`, and the request's `only-if-cached`/`max-age`/`min-fresh` directives.

If the cached entry is fresh it is served with no network call. If it is stale (or the request
demands revalidation) the filter sends a conditional request and, on a `304`, returns the cached
body with updated headers.

## Scenarios

### Fresh response with `Cache-Control: max-age`

The most common case: the server tells the cache how long a response may be reused. Subsequent
requests inside the window are served from memory and never reach the network.

Server:

```
HTTP/1.1 200 OK
Cache-Control: public, max-age=3600
Content-Length: 5

hello
```

```kotlin
val hits = AtomicInteger()
val server = HttpHandler {
    hits.incrementAndGet()
    Response(OK).body("hello").header("Cache-Control", "public, max-age=3600")
}
val client = ClientCacheFilters(timeSource = { now }).then(server)

client(Request(GET, "http://host/hello"))      // 1 network call, body "hello"
// advance the clock by 10 minutes
client(Request(GET, "http://host/hello"))      // served from cache, still 1 network call
```

The age of the served copy is reported in the `Age` response header.

### `Cache-Control: no-store` - never cache

A response marked `no-store` must not be kept anywhere. Every request hits the network:

```
HTTP/1.1 200 OK
Cache-Control: no-store

secret
```

```kotlin
client(Request(GET, "http://host/secret"))
client(Request(GET, "http://host/secret"))   // hits the network again
```

### `Cache-Control: no-cache` on the response - store but always revalidate

`no-cache` does not mean "don't cache" - it means "never serve from cache without checking with
the server first". The body is cached so a `304` can be served from it, but every request goes
over the wire:

```
HTTP/1.1 200 OK
Cache-Control: no-cache
ETag: "abc"

versioned-content
```

```kotlin
client(Request(GET, "http://host/versioned-content"))  // 200, cached
client(Request(GET, "http://host/versioned-content"))  // conditional request -> 304 -> cached body
```

### `Expires` freshness fallback

If there is no `Cache-Control` the cache falls back to the deprecated `Expires` date:

```
HTTP/1.1 200 OK
Date: Tue, 15 Nov 2026 08:12:31 GMT
Expires: Tue, 15 Nov 2026 18:12:31 GMT

hello
```

The response is considered fresh until `Expires`.

### Heuristic caching from `Last-Modified`

When there is no explicit freshness information at all, the cache applies the RFC 9111 heuristic:
10% of the time between `Last-Modified` and `Date`:

```
HTTP/1.1 200 OK
Date: Tue, 15 Nov 2026 08:12:31 GMT
Last-Modified: Tue, 15 Nov 2026 00:00:00 GMT

hello
```

Here the heuristic freshness lifetime is `(08:12 - 00:00) * 0.1` ≈ 49 minutes. Disable with
`ClientCacheFilters(heuristicCaching = false)`.

### Revalidation with `ETag` and `If-None-Match`

When a cached response becomes stale and carries an `ETag`, the filter revalidates by echoing it
in `If-None-Match`. A `304 Not Modified` is translated back into the cached representation:

```
HTTP/1.1 304 Not Modified
ETag: "abc"
Cache-Control: public, max-age=600
```

The original status (`200`), body, and `Content-Type` are retained; the fresh headers from the
`304` (and the recomputed `Content-Length`) are merged in, and the entry is re-stored so it is
fresh for another window. The caller only ever sees a `200`.

```kotlin
var hits = 0
val server = HttpHandler { req ->
    hits++
    if (req.header("If-None-Match") == "\"abc\"") {
        Response(NOT_MODIFIED).header("ETag", "\"abc\"").header("Cache-Control", "public, max-age=600")
    } else {
        Response(OK).body("hello").header("ETag", "\"abc\"").header("Cache-Control", "public, max-age=5")
    }
}
val client = ClientCacheFilters(timeSource = { now }).then(server)

client(Request(GET, "http://host/resource"))  // 200, body "hello", hits = 1
// advance past the 5s max-age
client(Request(GET, "http://host/resource"))  // 304 -> cached body "hello", hits = 2
client(Request(GET, "http://host/resource"))  // fresh again, hits = 2
```

### Revalidation with `Last-Modified` and `If-Modified-Since`

If there is no `ETag`, the filter uses `Last-Modified` and sends `If-Modified-Since` instead:

```
HTTP/1.1 200 OK
Last-Modified: Tue, 15 Nov 2026 08:00:00 GMT
Cache-Control: public, max-age=5

hello
```

```
GET /resource HTTP/1.1
If-Modified-Since: Tue, 15 Nov 2026 08:00:00 GMT
```

### `Age` header accelerates staleness

A response may already be old by the time it reaches us. Following RFC 9111 §4.2.3, the `Age`
header is added to the time spent in our cache, so a `max-age` window can expire early:

```
HTTP/1.1 200 OK
Cache-Control: public, max-age=100
Age: 90
```

The remaining freshness is only 10 seconds from the moment it was stored.

### `Vary` - cache per request-header combination

Responses that change based on request headers use `Vary`. The filter stores the request header
values it was generated from and only reuses the entry when the current request matches:

```
HTTP/1.1 200 OK
Cache-Control: public, max-age=3600
Vary: Accept-Language

français
```

```kotlin
client(Request(GET, "http://host/page").header("Accept-Language", "fr"))  // cached for fr
client(Request(GET, "http://host/page").header("Accept-Language", "fr"))  // cache hit
client(Request(GET, "http://host/page").header("Accept-Language", "en"))  // miss -> network
```

A `Vary: *` response is never reused (its value always fails to match, RFC 9111 §4.1), and a
single variant is kept per method+URI (the most recently stored write wins).

### Conditional requests from the client

Requests can also carry `Cache-Control` directives:

- `Request ... Cache-Control: no-cache` - force revalidation even if the cached response is fresh.
- `Request ... Cache-Control: no-store` - bypass the cache entirely.
- `Request ... Cache-Control: only-if-cached` - never touch the network; serve whatever is cached
  (even stale), or return `504 Gateway Timeout` if there is nothing.
- `Request ... Cache-Control: max-age=0` - revalidate (this is what browsers send on reload).
- `Request ... Cache-Control: max-stale=60` - accept a response up to 60 seconds past its
  freshness lifetime, and `max-stale` alone accepts any staleness.
- `Request ... Cache-Control: min-fresh=30` - only serve if at least 30 seconds of freshness
  remain.

```kotlin
client(Request(GET, "http://host/page").header("Cache-Control", "only-if-cached"))
// -> cached copy if present, otherwise 504 Gateway Timeout (no network call)
```

- `immutable` responses (an extension directive, from the browser ecosystem) are exempt from the
  reload (`max-age=0`) revalidation while they remain fresh.

### `stale-while-revalidate` and `stale-if-error`

Two companion directives soften staleness for resilience and speed:

- `stale-while-revalidate=N` - serve the stale copy for up to `N` seconds past its freshness
  lifetime without waiting on the network.
- `stale-if-error=N` - keep the stale copy usable, and if a revalidation attempt fails with a
  `5xx`, serve the stale copy instead of propagating the error.

```
HTTP/1.1 200 OK
Cache-Control: public, max-age=10, stale-while-revalidate=300

hello
```

```kotlin
// within max-age  -> served fresh, no network
// up to 300s past max-age -> still served without a network call
// beyond that -> revalidated over the network
```

Both are extension directives from [RFC 5861](https://www.rfc-editor.org/rfc/rfc5861.html),
referenced by RFC 9111 §4.2.4, and are honoured only when the stored response does not forbid it
(`no-cache`, `must-revalidate`, `proxy-revalidate`).

### Write methods invalidate the cache

Non-safe methods (`POST`, `PUT`, `DELETE`, `PATCH`, ...) are passed straight through and
invalidate any cached entry for the target URI, plus any `Location`/`Content-Location` target
(RFC 9111 §4.4), so a subsequent `GET` sees fresh data:

```kotlin
client(Request(GET, "http://host/thing"))    // cached
client(Request(POST, "http://host/thing"))   // invalidates the entry
client(Request(GET, "http://host/thing"))    // re-fetched, not served from cache
```

### `Authorization` requests

A response to a request carrying an `Authorization` header is only cached if the response also
has `public`, `s-maxage`, or `must-revalidate` (RFC 9111 §3.5):

```
HTTP/1.1 200 OK
Cache-Control: public, max-age=300

account-balance
```

```
HTTP/1.1 200 OK
Cache-Control: private, max-age=300        <- NOT cached for authenticated requests
max-age=300                                <- also NOT cached for authenticated requests
```

## Which responses are cached

By default only `GET` responses (and, from the cache, `HEAD` requests) with a heuristically
cacheable status code are stored - RFC 9111 §3 / §4.2.2: `200, 203, 204, 206, 300, 301, 308,
404, 405, 410, 414, 501`. Other statuses pass through untouched. The set is overridable via
`ClientCacheFilters(cacheableStatusCodes = ...)`.

## Observing decisions

A response replayed from the cache carries an up-to-date `Age` header, which is the simplest way
to distinguish a cache hit from a fresh network response. For richer telemetry, wrap the client
with the standard http4k event/tracing filters (e.g. `ClientFilters.ReportHttpTransaction`) - a
served-from-cache response will produce no outgoing transaction event.

## Limitations

- A single cached variant is kept per method+URI (last-writer-wins across `Vary` variants).
- Responses containing `Set-Cookie` are still cached, as a browser would.
- When serving inside a `stale-while-revalidate` window the filter does not trigger a background
  refresh (it is a synchronous filter); the entry is revalidated on the first request that falls
  outside the window.