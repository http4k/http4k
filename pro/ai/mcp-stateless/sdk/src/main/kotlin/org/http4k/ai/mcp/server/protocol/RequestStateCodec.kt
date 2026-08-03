/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.server.protocol.RequestStateCodec.Companion.Hmac
import org.http4k.ai.mcp.server.protocol.RequestStateCodec.Companion.None
import org.http4k.security.Sha256
import org.http4k.util.Hex
import java.security.MessageDigest
import java.util.Base64

/**
 * Integrity protection for the opaque MRTR `requestState` the client round-trips between input-required rounds.
 * The server [sign]s the state on the way out and [verify]s it on the way back — a tampered token fails
 * verification and the request is rejected before the handler runs. Pluggable so operators can supply their own
 * scheme; [None] and [Hmac] are provided.
 */
interface RequestStateCodec {
    fun sign(state: String): String

    /** Recover the original state, or null if the token failed integrity verification. */
    fun verify(token: String): String?

    companion object {
        /** Passthrough — no integrity (the back-compat default; safe across instances as there is no key). */
        val None = object : RequestStateCodec {
            override fun sign(state: String) = state
            override fun verify(token: String) = token
        }

        /**
         * Signed-but-readable token `base64(state).hexHmac(state)` (a JWT-style envelope; integrity, not
         * secrecy). The same [key] must be shared across instances that serve rounds of the same conversation.
         */
        fun Hmac(key: ByteArray): RequestStateCodec = object : RequestStateCodec {
            override fun sign(state: String) =
                Base64.getEncoder().encodeToString(state.toByteArray()) + "." + Hex.hex(Sha256.hmac(key, state))

            override fun verify(token: String): String? {
                val parts = token.split(".", limit = 2)
                if (parts.size != 2) return null
                val state = runCatching { String(Base64.getDecoder().decode(parts[0])) }.getOrNull()
                if (state == null) return null
                val expected = Hex.hex(Sha256.hmac(key, state))
                return if (MessageDigest.isEqual(expected.toByteArray(), parts[1].toByteArray())) state else null
            }
        }
    }
}
