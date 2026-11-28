package org.http4k.webhook.signing

import dev.forkhandles.values.Constraint.Companion.Size
import dev.forkhandles.values.ConstraintProblem
import dev.forkhandles.values.Message
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.webhook.signing.HmacSha256SigningSecret.Companion.PREFIX
import java.util.Base64

class HmacSha256SigningSecret private constructor(value: String) : StringValue(value) {
    fun withNoPrefix() = value.removePrefix(PREFIX)

    companion object : StringValueFactory<HmacSha256SigningSecret>(::HmacSha256SigningSecret, validation = { it: String ->
        when (Base64.getDecoder().decode(it.removePrefix(PREFIX)).size) {
            in 24..64 -> null
            else -> ConstraintProblem(Size, Message("decoded size must be between 24 and 64 bytes"), it)
        }
    }) {
        internal const val PREFIX = "whsec_"

        /**
         * Encode a value without a secret signing prefix. Use "of()" for pre-encoded secrets
         */
        fun encode(unencoded: String) = encode(unencoded.toByteArray())
        fun encode(unencoded: ByteArray) =
            HmacSha256SigningSecret.of(PREFIX + Base64.getEncoder().encodeToString(unencoded))
    }
}
