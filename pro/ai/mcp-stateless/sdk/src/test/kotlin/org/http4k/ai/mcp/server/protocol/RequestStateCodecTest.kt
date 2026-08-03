/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class RequestStateCodecTest {

    private val hmac = RequestStateCodec.Hmac("secret-key".toByteArray())

    @Test
    fun `hmac round-trips a signed state`() {
        assertThat(hmac.verify(hmac.sign("some-state")), equalTo("some-state"))
    }

    @Test
    fun `hmac rejects a tampered token`() {
        assertThat(hmac.verify(hmac.sign("some-state") + "-TAMPERED"), absent())
    }

    @Test
    fun `hmac rejects a token signed with a different key`() {
        val other = RequestStateCodec.Hmac("different-key".toByteArray())
        assertThat(hmac.verify(other.sign("some-state")), absent())
    }

    @Test
    fun `hmac rejects a structurally invalid token`() {
        assertThat(hmac.verify("not-a-token"), absent())
    }

    @Test
    fun `None passes the state through unchanged both ways`() {
        assertThat(RequestStateCodec.None.sign("x"), equalTo("x"))
        assertThat(RequestStateCodec.None.verify("x"), equalTo("x"))
    }
}
