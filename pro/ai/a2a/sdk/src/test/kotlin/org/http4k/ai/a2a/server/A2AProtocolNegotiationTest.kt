/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.model.AgentExtension
import org.http4k.ai.a2a.protocol.ProtocolVersion
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.junit.jupiter.api.Test

class A2AProtocolNegotiationTest {

    private val ok: HttpHandler = { Response(OK) }

    @Test
    fun `passes through when no version header`() {
        val filter = A2AProtocolNegotiation(AgentCapabilities())
        assertThat(filter.then(ok)(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `passes through when supported version`() {
        val filter = A2AProtocolNegotiation(AgentCapabilities())
        val request = Request(GET, "/").header("A2A-Version", "1.0.0")
        assertThat(filter.then(ok)(request).status, equalTo(OK))
    }

    @Test
    fun `rejects unsupported version`() {
        val filter = A2AProtocolNegotiation(AgentCapabilities())
        val request = Request(GET, "/").header("A2A-Version", "99.0.0")
        assertThat(filter.then(ok)(request).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `rejects unsupported version with custom supported set`() {
        val filter = A2AProtocolNegotiation(AgentCapabilities(), supportedVersions = setOf(ProtocolVersion.of("2.0.0")))
        val request = Request(GET, "/").header("A2A-Version", "1.0.0")
        assertThat(filter.then(ok)(request).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `passes through when no required extensions`() {
        val filter = A2AProtocolNegotiation(AgentCapabilities())
        assertThat(filter.then(ok)(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `passes through when no required extensions even without header`() {
        val filter = A2AProtocolNegotiation(
            AgentCapabilities(extensions = listOf(AgentExtension(uri = Uri.of("https://example.com/ext"), required = false)))
        )
        assertThat(filter.then(ok)(Request(GET, "/")).status, equalTo(OK))
    }

    @Test
    fun `rejects when required extension missing from header`() {
        val filter = A2AProtocolNegotiation(
            AgentCapabilities(extensions = listOf(AgentExtension(uri = Uri.of("https://example.com/ext"), required = true)))
        )
        assertThat(filter.then(ok)(Request(GET, "/")).status, equalTo(BAD_REQUEST))
    }

    @Test
    fun `passes through when required extension present in header`() {
        val filter = A2AProtocolNegotiation(
            AgentCapabilities(extensions = listOf(AgentExtension(uri = Uri.of("https://example.com/ext"), required = true)))
        )
        val request = Request(GET, "/").header("A2A-Extensions", "https://example.com/ext")
        assertThat(filter.then(ok)(request).status, equalTo(OK))
    }

    @Test
    fun `handles multiple extension headers`() {
        val filter = A2AProtocolNegotiation(
            AgentCapabilities(extensions = listOf(
                AgentExtension(uri = Uri.of("https://example.com/a"), required = true),
                AgentExtension(uri = Uri.of("https://example.com/b"), required = true)
            ))
        )
        val request = Request(GET, "/")
            .header("A2A-Extensions", "https://example.com/a")
            .header("A2A-Extensions", "https://example.com/b")
        assertThat(filter.then(ok)(request).status, equalTo(OK))
    }

    @Test
    fun `rejects when one of multiple required extensions missing`() {
        val filter = A2AProtocolNegotiation(
            AgentCapabilities(extensions = listOf(
                AgentExtension(uri = Uri.of("https://example.com/a"), required = true),
                AgentExtension(uri = Uri.of("https://example.com/b"), required = true)
            ))
        )
        val request = Request(GET, "/").header("A2A-Extensions", "https://example.com/a")
        assertThat(filter.then(ok)(request).status, equalTo(BAD_REQUEST))
    }
}
