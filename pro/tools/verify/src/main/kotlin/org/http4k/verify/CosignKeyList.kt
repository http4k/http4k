/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.verify.KeyStatus.revoked
import java.time.Instant

class KeyId private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<KeyId>(::KeyId)
}

class KeyFingerprint private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<KeyFingerprint>(::KeyFingerprint)
}

@Suppress("EnumEntryName")
enum class KeyStatus { active, retired, revoked }

data class CosignKey(
    val kid: String,
    val fingerprint: String,
    val publicKey: String,
    val status: KeyStatus,
    val validFrom: Instant,
    val validTo: Instant? = null
) {
    val keyId get() = KeyId.of(kid)
    val keyFingerprint get() = KeyFingerprint.of(fingerprint)
}

data class CosignKeyList(val schemaVersion: Int, val keys: List<CosignKey>) {
    fun validated(): CosignKeyList {
        if (schemaVersion != 1) error("Unsupported cosign-keys.json schema version: $schemaVersion (expected 1)")
        return this
    }

    fun nonRevoked() = keys.filter { it.status != revoked }
}
