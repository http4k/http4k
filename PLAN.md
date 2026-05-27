# OpenFeature: client-side cache + lens DSL for flag reads

## Context

Two related additions to the existing OpenFeature work:

1. **Cache** — flag evaluations are read repeatedly per request (and often unchanged across requests). The connect `OpenFeature` client today hits OFREP on every call. Add a caching decorator backed by http4k-connect's `Storage<T>` abstraction so users can plug in `Storage.InMemory()`, `Storage.Redis(...)`, etc.

2. **Flag-lens DSL** — handlers currently read `OPENFEATURE_CONTEXT_KEY(req)` and then call `client.getBooleanValue(key, default, ctx)`. Replace with the http4k lens idiom: declare each flag once at app boot, read it on a request like a header lens. Example: `OpenFeature.boolean().optional("dark-mode")` returns a `Lens<Request, Boolean?>` whose read chains `OPENFEATURE_CONTEXT_KEY` + a sibling `OPENFEATURE_CLIENT_KEY` and calls the SDK.

## Design

### (1) Cache — `connect/openfeature/client`

A new top-level decorator that implements `OpenFeature`:

```kotlin
fun OpenFeature.cached(
    storage: Storage<CachedEvaluation> = Storage.InMemory(),
    ttl: Duration,
    clock: Clock = Clock.systemUTC()
): OpenFeature = CachedOpenFeature(this, storage, ttl, clock)

@JsonSerializable
data class CachedEvaluation(val value: EvaluationSuccess, val expiresAt: Instant)

internal class CachedOpenFeature(
    private val delegate: OpenFeature,
    private val storage: Storage<CachedEvaluation>,
    private val ttl: Duration,
    private val clock: Clock
) : OpenFeature {
    override fun <R : Any> invoke(action: OpenFeatureAction<R>): Result<R, RemoteFailure> =
        when (action) {
            is EvaluateFlag -> @Suppress("UNCHECKED_CAST") (evaluateFlag(action) as Result<R, RemoteFailure>)
            else -> delegate(action)
        }

    private fun evaluateFlag(action: EvaluateFlag): Result<EvaluationSuccess, RemoteFailure> {
        val key = cacheKey(action.key, action.context)
        storage[key]?.takeIf { it.expiresAt.isAfter(clock.instant()) }?.let { return Success(it.value) }
        return delegate(action).also { result ->
            if (result is Success) storage[key] = CachedEvaluation(result.value, clock.instant().plus(ttl))
        }
    }

    private fun cacheKey(flag: FlagKey, context: EvaluationContext): String =
        "$flag:${OpenFeatureMoshi.asJson(context)}"
}
```

Key points:
- **Scope: `EvaluateFlag` only.** `EvaluateAllFlags` passes through to delegate; bulk caching can be a follow-on. Most server use will hit single-flag reads via the lens DSL anyway.
- **Cache key**: `"flagKey:jsonOfContext"`. The whole `EvaluationContext` is part of the key — users should avoid putting request-unique attrs into the context, which is the standard caching gotcha for OpenFeature.
- **TTL via wrapper**: `Storage<T>` has no native TTL, so the value is `CachedEvaluation(value, expiresAt)` and we evict lazily on read (stale entries are simply re-fetched and overwritten). No background sweeper needed.
- **`OpenFeatureMoshi.asJson`** handles the serialization. Kotshi's adapter for `EvaluationContext` already exists, so `CachedEvaluation` just needs `@JsonSerializable` to participate.

Build dependency: `connect/openfeature/client/build.gradle.kts` adds `api(project(":http4k-connect-storage-core"))`.

### (2) Flag-lens DSL — `core/ops/openfeature`

A new `OpenFeature` object that mirrors http4k's lens DSL (`Query.int().optional("age")`, `Header.localDate().defaulted("when", LocalDate.now())`):

```kotlin
// org.http4k.lens.openFeature (new file)

object OpenFeature {
    fun boolean() = OpenFeatureLensSpec(::booleanEvaluate)
    fun string()  = OpenFeatureLensSpec(::stringEvaluate)
    fun int()     = OpenFeatureLensSpec(::intEvaluate)
    fun double()  = OpenFeatureLensSpec(::doubleEvaluate)
    fun obj()     = OpenFeatureLensSpec(::objectEvaluate)

    // derived types via .map on the underlying spec
    fun long()      = int().map(Int::toLong, Long::toInt)
    fun localDate() = string().map(LocalDate::parse) { it.toString() }
    fun instant()   = string().map(Instant::parse) { it.toString() }
}

class OpenFeatureLensSpec<UNDER : Any, OUT : Any> internal constructor(
    private val getDetails: (Client, String, UNDER, EvaluationContext) -> FlagEvaluationDetails<UNDER>,
    private val under: TypeBridge<UNDER, OUT>
) {
    fun defaulted(key: String, default: OUT): Lens<Request, OUT> = lens(key) { client, ctx ->
        val details = getDetails(client, key, under.toUnder(default), ctx)
        if (details.errorCode != null) default else under.toOut(details.value)
    }

    fun optional(key: String): Lens<Request, OUT?> = lens(key) { client, ctx ->
        val details = getDetails(client, key, under.zero, ctx)
        if (details.errorCode != null) null else under.toOut(details.value)
    }

    fun required(key: String): Lens<Request, OUT> = lens(key) { client, ctx ->
        val details = getDetails(client, key, under.zero, ctx)
        if (details.errorCode != null) throw LensFailure(Missing(meta(key)), target = null)
        under.toOut(details.value)
    }

    fun <NEW : Any> map(forward: (OUT) -> NEW, backward: (NEW) -> OUT): OpenFeatureLensSpec<UNDER, NEW> =
        OpenFeatureLensSpec(getDetails, under.map(forward, backward))

    private fun <T> lens(key: String, read: (Client, EvaluationContext) -> T): Lens<Request, T> =
        Lens(meta(key)) { req -> read(OPENFEATURE_CLIENT_KEY(req), OPENFEATURE_CONTEXT_KEY(req)) }
}
```

`TypeBridge<UNDER, OUT>` is a small internal helper carrying `(toOut, toUnder, zero)` — keeps `map` chaining clean without exploding type signatures.

Usage in handlers:

```kotlin
// at app boot
val darkMode    = OpenFeature.boolean().optional("dark-mode")        // Lens<Request, Boolean?>
val greeting    = OpenFeature.string().defaulted("greeting", "hi")    // Lens<Request, String>
val maxItems    = OpenFeature.int().required("max-items")             // Lens<Request, Int>  (throws if missing)
val releaseDate = OpenFeature.localDate().defaulted("release", LocalDate.now())

val app = ServerFilters.UseOpenFeatureClient(sdkClient)
    .then(ServerFilters.PopulateOpenFeatureContext { req -> /* ... */ })
    .then { req ->
        Response(OK).body(if (darkMode(req) == true) "dark" else "light")
    }
```

### (2a) Client request key + filter

The flag lens reads two request-scoped values: the SDK `Client` and the `EvaluationContext`. Context already has `OPENFEATURE_CONTEXT_KEY` + `ServerFilters.PopulateOpenFeatureContext`. Add a sibling for the client:

```kotlin
// org.http4k.filter.openFeatureExtensions (existing file)
val OPENFEATURE_CLIENT_KEY = RequestKey.required<Client>("HTTP4K_OPENFEATURE_CLIENT")

fun ServerFilters.UseOpenFeatureClient(client: Client) = Filter { next ->
    { req -> next(req.with(OPENFEATURE_CLIENT_KEY of client)) }
}
```

Apps wire both filters in front of the handler. Either order works; the flag lens only reads them, never sets them.

## Critical files

Create:
- `connect/openfeature/client/src/main/kotlin/org/http4k/connect/openfeature/CachedOpenFeature.kt` — decorator + `cached()` factory + `CachedEvaluation`.
- `core/ops/openfeature/src/main/kotlin/org/http4k/lens/openFeature.kt` — `OpenFeature` object + `OpenFeatureLensSpec` + `TypeBridge`.
- `connect/openfeature/client/src/test/kotlin/org/http4k/connect/openfeature/CachedOpenFeatureTest.kt` — hits FakeOpenFeature, asserts hit/miss counts via a counting wrapper.
- `core/ops/openfeature/src/test/kotlin/org/http4k/lens/OpenFeatureLensTest.kt` — full filter chain + FakeOpenFeature + Http4kOpenFeatureProvider, asserts each lens variant.

Modify:
- `connect/openfeature/client/build.gradle.kts` — add `api(project(":http4k-connect-storage-core"))`.
- `core/ops/openfeature/src/main/kotlin/org/http4k/filter/openFeatureExtensions.kt` — add `OPENFEATURE_CLIENT_KEY` and `ServerFilters.UseOpenFeatureClient`.

Reuse:
- `org.http4k.connect.openfeature.OpenFeatureMoshi.asJson(...)` — serialise the EvaluationContext into the cache key.
- `org.http4k.connect.storage.Storage<T>` — backing for the cache; `Storage.InMemory()` is the default.
- `org.http4k.filter.OPENFEATURE_CONTEXT_KEY` (existing) — read by the flag lens.
- `dev.openfeature.sdk.Client.getBooleanDetails(...)` etc — provides `FlagEvaluationDetails<T>` with `errorCode` so the lens can distinguish "missing" from "valid".

## Verification

1. `./gradlew :http4k-connect-openfeature:test :http4k-ops-openfeature:test` — both new test files pass.
2. Cache: test asserts that two identical `EvaluateFlag` calls (same key, same context) hit the delegate only once within TTL, and that a third call after the TTL elapses (via injected `Clock`) re-fetches.
3. Cache: test asserts that different `EvaluationContext`s for the same flag key produce independent cache entries (so per-user variation works).
4. Cache: test asserts `EvaluateAllFlags` passes through (delegate hit count increments each call).
5. Lens DSL: integration test wires `FakeOpenFeature` → `Http4kOpenFeatureProvider` → `OpenFeatureAPI.getInstance().setProvider(...)` → `OpenFeatureAPI.getInstance().client`, installs both `UseOpenFeatureClient` and `PopulateOpenFeatureContext` filters, seeds flag rules + defaults, then exercises `defaulted`, `optional`, and `required` for `boolean`, `string`, `int`, `long`, `localDate` lenses. Asserts that `required` throws `LensFailure` for a missing flag and that `optional` returns null for the same.
6. Manual smoke: a `main` that wires the chain end-to-end (FakeOpenFeature in-process) and demonstrates the handler-side `darkMode(req)` syntax — confirms no API rough edges.
