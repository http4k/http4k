/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64

class KeyResolverTest {

    private val keyPair1 = generateKeyPair()
    private val keyPair2 = generateKeyPair()

    private val fingerprint1 = KeyFingerprint.of("sha256:" + keyPair1.public.encoded.sha256Hex())
    private val fingerprint2 = KeyFingerprint.of("sha256:" + keyPair2.public.encoded.sha256Hex())

    private val pem1 = toPem(keyPair1.public.encoded)
    private val pem2 = toPem(keyPair2.public.encoded)

    private val keyList = CosignKeyList(
        schemaVersion = 1,
        keys = listOf(
            CosignKey("key-2025", fingerprint1.value, pem1, KeyStatus.active, Instant.parse("2025-01-01T00:00:00Z")),
            CosignKey(
                "key-2024",
                fingerprint2.value,
                pem2,
                KeyStatus.retired,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2025-06-30T23:59:59Z")
            )
        )
    )

    private val resolver = KeyResolver(keyList)

    @Test
    fun `resolves key by fingerprint`() {
        val result = resolver.resolve(fingerprint1)

        assertThat(result.key, equalTo(keyPair1.public))
        assertThat(result.kid, equalTo(KeyId.of("key-2025")))
    }

    @Test
    fun `resolves second key by fingerprint`() {
        val result = resolver.resolve(fingerprint2)

        assertThat(result.key, equalTo(keyPair2.public))
        assertThat(result.kid, equalTo(KeyId.of("key-2024")))
    }

    @Test
    fun `rejects revoked key`() {
        val revokedKeyList = CosignKeyList(
            schemaVersion = 1,
            keys = listOf(
                CosignKey("key-revoked", fingerprint1.value, pem1, KeyStatus.revoked, Instant.parse("2025-01-01T00:00:00Z"))
            )
        )

        assertThat(
            { KeyResolver(revokedKeyList).resolve(fingerprint1) },
            throws<KeyRevokedException>()
        )
    }

    @Test
    fun `throws for unknown fingerprint`() {
        assertThat(
            { resolver.resolve(KeyFingerprint.of("sha256:unknown")) },
            throws<IllegalStateException>()
        )
    }

    private fun generateKeyPair() = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private fun toPem(encoded: ByteArray) =
        "-----BEGIN PUBLIC KEY-----\n${Base64.getEncoder().encodeToString(encoded)}\n-----END PUBLIC KEY-----"
}
