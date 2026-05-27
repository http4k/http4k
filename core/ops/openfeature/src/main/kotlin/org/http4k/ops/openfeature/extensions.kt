package org.http4k.ops.openfeature

import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.EvaluationContext
import dev.openfeature.sdk.Value
import org.http4k.connect.RemoteFailure
import org.http4k.connect.openfeature.model.ErrorCode.FLAG_NOT_FOUND
import org.http4k.connect.openfeature.model.ErrorCode.GENERAL
import org.http4k.connect.openfeature.model.ErrorCode.INVALID_CONTEXT
import org.http4k.connect.openfeature.model.ErrorCode.PARSE_ERROR
import org.http4k.connect.openfeature.model.ErrorCode.PROVIDER_NOT_READY
import org.http4k.connect.openfeature.model.ErrorCode.TARGETING_KEY_MISSING
import org.http4k.connect.openfeature.model.ErrorCode.TYPE_MISMATCH
import org.http4k.connect.openfeature.model.EvaluationFailure
import org.http4k.connect.openfeature.model.TargetingKey
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.connect.openfeature.model.EvaluationContext as ConnectEvaluationContext

internal fun EvaluationContext?.toHttp4k(): ConnectEvaluationContext {
    val targetingKey = this?.targetingKey?.takeIf { it.isNotBlank() }?.let(TargetingKey::of)
    val attributes = this?.asUnmodifiableMap()
        ?.filterKeys { it != "targetingKey" }
        ?.mapValues { (_, v) -> v.valueToAny() }
        ?: emptyMap()
    return targetingKey
        ?.let { ConnectEvaluationContext(mapOf("targetingKey" to it.value) + attributes) }
        ?: ConnectEvaluationContext(attributes)
}

private fun Value?.valueToAny(): Any? = when {
    this == null || isNull -> null
    isString -> asString()
    isBoolean -> asBoolean()
    isNumber -> asDouble()
    isList -> asList()?.map { it.valueToAny() }
    isStructure -> asStructure()?.asUnmodifiableMap()?.mapValues { (_, v) -> v.valueToAny() }
    isInstant -> asInstant()?.toString()
    else -> null
}

internal fun toSdkErrorCode(failure: RemoteFailure, parsed: EvaluationFailure?): ErrorCode {
    parsed?.errorCode?.let { return mapErrorCode(it) }
    return when (failure.status) {
        NOT_FOUND -> ErrorCode.FLAG_NOT_FOUND
        UNAUTHORIZED, FORBIDDEN -> ErrorCode.PROVIDER_NOT_READY
        BAD_REQUEST -> ErrorCode.INVALID_CONTEXT
        else -> ErrorCode.GENERAL
    }
}

private fun mapErrorCode(code: org.http4k.connect.openfeature.model.ErrorCode): ErrorCode = when (code) {
    FLAG_NOT_FOUND -> ErrorCode.FLAG_NOT_FOUND
    PARSE_ERROR -> ErrorCode.PARSE_ERROR
    TYPE_MISMATCH -> ErrorCode.TYPE_MISMATCH
    TARGETING_KEY_MISSING -> ErrorCode.TARGETING_KEY_MISSING
    INVALID_CONTEXT -> ErrorCode.INVALID_CONTEXT
    PROVIDER_NOT_READY -> ErrorCode.PROVIDER_NOT_READY
    GENERAL -> ErrorCode.GENERAL
}
