package org.http4k.connect.openfeature

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.openfeature.action.EvaluateAllFlags
import org.http4k.connect.openfeature.action.EvaluateFlag
import org.http4k.connect.openfeature.model.BulkEvaluationSuccess
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.EvaluationSuccess
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import se.ansman.kotshi.JsonSerializable
import java.time.Clock
import java.time.Duration
import java.time.Instant

@JsonSerializable
data class CachedEvaluation(val value: EvaluationSuccess, val expiresAt: Instant)

@JsonSerializable
data class CachedBulkEvaluation(val value: BulkEvaluationSuccess, val expiresAt: Instant)

/**
 * Wraps an OpenFeature instance and caches the results for a given TTL.
 */
fun OpenFeature.Companion.Cached(
    delegate: OpenFeature,
    flagStorage: Storage<CachedEvaluation> = Storage.InMemory(),
    bulkStorage: Storage<CachedBulkEvaluation> = Storage.InMemory(),
    ttl: Duration = Duration.ofHours(1),
    clock: Clock = Clock.systemUTC()
): OpenFeature = object : OpenFeature {

    @Suppress("UNCHECKED_CAST")
    override fun <R : Any> invoke(action: OpenFeatureAction<R>): Result<R, RemoteFailure> = when (action) {
        is EvaluateFlag -> evaluateFlag(action)
        is EvaluateAllFlags -> evaluateAllFlags(action)
        else -> delegate(action)
    } as Result<R, RemoteFailure>

    private fun evaluateFlag(action: EvaluateFlag): Result<EvaluationSuccess, RemoteFailure> {
        val key = "${action.key}:${contextKey(action.context)}"
        flagStorage[key]?.takeIf { it.expiresAt.isAfter(clock.instant()) }?.let { return Success(it.value) }
        return delegate(action).also { result ->
            if (result is Success) flagStorage[key] = CachedEvaluation(result.value, clock.instant().plus(ttl))
        }
    }

    private fun evaluateAllFlags(action: EvaluateAllFlags): Result<BulkEvaluationSuccess, RemoteFailure> {
        val key = contextKey(action.context)
        bulkStorage[key]?.takeIf { it.expiresAt.isAfter(clock.instant()) }?.let { return Success(it.value) }
        return delegate(action).also { result ->
            if (result is Success) bulkStorage[key] = CachedBulkEvaluation(result.value, clock.instant().plus(ttl))
        }
    }

    private fun contextKey(context: EvaluationContext): String = OpenFeatureMoshi.asFormatString(context)
}
