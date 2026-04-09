/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.verify.KeyStatus.revoked
import java.security.PublicKey

data class ResolvedKey(val kid: KeyId, val key: PublicKey)

class KeyRevokedException(kid: KeyId) : RuntimeException("Key '$kid' has been revoked")

class KeyResolver(private val keyList: CosignKeyList) {

    fun resolve(fingerprint: KeyFingerprint): ResolvedKey {
        val cosignKey = keyList.keys.firstOrNull { it.keyFingerprint == fingerprint }
            ?: error("No key found for fingerprint: $fingerprint")

        if (cosignKey.status == revoked) {
            throw KeyRevokedException(cosignKey.keyId)
        }

        return ResolvedKey(cosignKey.keyId, PublicKeyLoader.parsePem(cosignKey.publicKey))
    }
}
