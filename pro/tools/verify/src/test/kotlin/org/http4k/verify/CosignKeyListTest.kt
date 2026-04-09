/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.format.Moshi
import org.junit.jupiter.api.Test
import java.time.Instant

class CosignKeyListTest {

    @Test
    fun `deserializes key list from JSON`() {
        val json = """
        {
          "schemaVersion": 1,
          "keys": [
            {
              "kid": "http4k-signing-2025",
              "fingerprint": "sha256:abc123",
              "publicKey": "-----BEGIN PUBLIC KEY-----\nMFkwtest\n-----END PUBLIC KEY-----",
              "validFrom": "2025-01-01T00:00:00Z",
              "validTo": null,
              "status": "active"
            },
            {
              "kid": "http4k-signing-2024",
              "fingerprint": "sha256:def456",
              "publicKey": "-----BEGIN PUBLIC KEY-----\nMFkwold\n-----END PUBLIC KEY-----",
              "validFrom": "2024-01-01T00:00:00Z",
              "validTo": "2025-06-30T23:59:59Z",
              "status": "retired"
            }
          ]
        }
        """.trimIndent()

        val keyList = Moshi.asA<CosignKeyList>(json)

        assertThat(keyList.schemaVersion, equalTo(1))
        assertThat(keyList.keys.size, equalTo(2))
        assertThat(keyList.keys[0].kid, equalTo("http4k-signing-2025"))
        assertThat(keyList.keys[0].fingerprint, equalTo("sha256:abc123"))
        assertThat(keyList.keys[0].status, equalTo(KeyStatus.active))
        assertThat(keyList.keys[0].validTo, equalTo(null))
        assertThat(keyList.keys[1].kid, equalTo("http4k-signing-2024"))
        assertThat(keyList.keys[1].status, equalTo(KeyStatus.retired))
        assertThat(keyList.keys[1].validTo, equalTo(Instant.parse("2025-06-30T23:59:59Z")))
    }

    @Test
    fun `validates schema version`() {
        val keyList = CosignKeyList(schemaVersion = 2, keys = emptyList())

        assertThat({ keyList.validated() }, throws<IllegalStateException>())
    }

    @Test
    fun `validates accepted schema version`() {
        val keyList = CosignKeyList(schemaVersion = 1, keys = emptyList())

        assertThat(keyList.validated().schemaVersion, equalTo(1))
    }

    @Test
    fun `filters non-revoked keys`() {
        val keyList = CosignKeyList(
            schemaVersion = 1,
            keys = listOf(
                CosignKey("k1", "sha256:a", "pem1", KeyStatus.active, Instant.parse("2025-01-01T00:00:00Z")),
                CosignKey("k2", "sha256:b", "pem2", KeyStatus.revoked, Instant.parse("2024-01-01T00:00:00Z")),
                CosignKey("k3", "sha256:c", "pem3", KeyStatus.retired, Instant.parse("2023-01-01T00:00:00Z"))
            )
        )

        assertThat(keyList.nonRevoked().size, equalTo(2))
        assertThat(keyList.nonRevoked().map { it.kid }, equalTo(listOf("k1", "k3")))
    }
}
