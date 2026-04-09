/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import java.io.File
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

data class LoadedKey(val key: PublicKey, val pemText: String)

class PublicKeyLoader(
    private val publicKeyFile: File?,
    private val log: (String) -> Unit,
    private val client: HttpHandler
) {
    fun load(): LoadedKey {
        val pem = when {
            publicKeyFile != null -> publicKeyFile.readText()
            else -> {
                log("Downloading public key from https://http4k.org/cosign.pub")
                val response = client(Request(GET, "https://http4k.org/cosign.pub"))
                if (response.status != OK) error("Failed to download public key: ${response.status}")
                response.bodyString()
            }
        }

        return LoadedKey(parsePem(pem), pem)
    }

    companion object {
        fun parsePem(pemContent: String): PublicKey =
            KeyFactory.getInstance("EC").generatePublic(
                X509EncodedKeySpec(
                    Base64.getDecoder().decode(
                        pemContent
                            .replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replace("\\s+".toRegex(), "")
                    )
                )
            )
    }
}
