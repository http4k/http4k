/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.io.File
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class PublicKeyLoaderTest {

    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private val pem = "-----BEGIN PUBLIC KEY-----\n" +
        Base64.getEncoder().encodeToString(keyPair.public.encoded) +
        "\n-----END PUBLIC KEY-----"

    @Test
    fun `loads key from file`() {
        val file = File.createTempFile("cosign", ".pub").apply {
            deleteOnExit()
            writeText(pem)
        }

        val (key, pemText) = PublicKeyLoader(publicKeyFile = file, log = {}).load()

        assertThat(key.algorithm, equalTo("EC"))
        assertThat(pemText, equalTo(pem))
    }

    @Test
    fun `downloads key when no file provided`() {
        val logged = mutableListOf<String>()
        val (key, pemText) = PublicKeyLoader(
            publicKeyFile = null,
            log = { logged += it },
            client = { Response(OK).body(pem) }
        ).load()

        assertThat(key.algorithm, equalTo("EC"))
        assertThat(pemText, equalTo(pem))
        assertThat(logged.size, equalTo(1))
    }

    @Test
    fun `parses PEM public key`() {
        val key = PublicKeyLoader.parsePem(pem)
        assertThat(key.algorithm, equalTo("EC"))
    }

    @Test
    fun `fails on non-OK response`() {
        val loader = PublicKeyLoader(
            publicKeyFile = null,
            log = {},
            client = { Response(NOT_FOUND) }
        )

        assertThat({ loader.load() }, throws<IllegalArgumentException>())
    }
}
