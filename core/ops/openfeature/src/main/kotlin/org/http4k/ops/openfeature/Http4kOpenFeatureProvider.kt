package org.http4k.ops.openfeature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.FeatureProvider
import dev.openfeature.sdk.Metadata
import dev.openfeature.sdk.ProviderEvaluation
import dev.openfeature.sdk.Reason
import dev.openfeature.sdk.Value
import org.http4k.connect.RemoteFailure
import org.http4k.connect.openfeature.OpenFeature
import org.http4k.connect.openfeature.OpenFeatureMoshi
import org.http4k.connect.openfeature.action.EvaluateFlag
import org.http4k.connect.openfeature.model.EvaluationFailure
import org.http4k.connect.openfeature.model.EvaluationSuccess
import org.http4k.connect.openfeature.model.FlagKey

class Http4kOpenFeatureProvider(private val client: OpenFeature) : FeatureProvider {

    override fun getMetadata() = Metadata { "http4k-ops-openfeature" }

    override fun getBooleanEvaluation(key: String, defaultValue: Boolean?, ctx: EvaluationContext?) =
        evaluate(key, defaultValue, ctx) { it as? Boolean ?: throw TypeMismatch() }

    override fun getStringEvaluation(key: String, defaultValue: String?, ctx: EvaluationContext?) =
        evaluate(key, defaultValue, ctx) { it as? String ?: throw TypeMismatch() }

    override fun getIntegerEvaluation(key: String, defaultValue: Int?, ctx: EvaluationContext?) =
        evaluate(key, defaultValue, ctx) { (it as? Number)?.toInt() ?: throw TypeMismatch() }

    override fun getDoubleEvaluation(key: String, defaultValue: Double?, ctx: EvaluationContext?) =
        evaluate(key, defaultValue, ctx) { (it as? Number)?.toDouble() ?: throw TypeMismatch() }

    override fun getObjectEvaluation(key: String, defaultValue: Value?, ctx: EvaluationContext?) =
        evaluate(key, defaultValue, ctx) { Value.objectToValue(it) }

    private fun <T> evaluate(
        key: String,
        defaultValue: T?,
        ctx: EvaluationContext?,
        coerce: (Any?) -> T
    ): ProviderEvaluation<T> =
        when (val result = client(EvaluateFlag(FlagKey.of(key), ctx.toHttp4k()))) {
            is Success -> success(result.value, defaultValue, coerce)
            is Failure -> failure(result.reason, defaultValue)
        }

    private fun <T> success(
        eval: EvaluationSuccess,
        defaultValue: T?,
        coerce: (Any?) -> T
    ): ProviderEvaluation<T> = try {
        val coerced = coerce(eval.value)
        ProviderEvaluation.builder<T>()
            .value(coerced)
            .variant(eval.variant)
            .reason(eval.reason?.name)
            .build()
    } catch (_: TypeMismatch) {
        ProviderEvaluation.builder<T>()
            .value(defaultValue)
            .reason(Reason.ERROR.name)
            .errorCode(ErrorCode.TYPE_MISMATCH)
            .errorMessage("Flag '${eval.key}' had value of unexpected type")
            .build()
    }

    private fun <T> failure(failure: RemoteFailure, defaultValue: T?): ProviderEvaluation<T> {
        val parsed =
            failure.message?.let { runCatching { OpenFeatureMoshi.asA(it, EvaluationFailure::class) }.getOrNull() }
        return ProviderEvaluation.builder<T>()
            .value(defaultValue)
            .reason(Reason.ERROR.name)
            .errorCode(toSdkErrorCode(failure, parsed))
            .errorMessage(parsed?.errorDetails ?: failure.message)
            .build()
    }
}

private class TypeMismatch : RuntimeException()
