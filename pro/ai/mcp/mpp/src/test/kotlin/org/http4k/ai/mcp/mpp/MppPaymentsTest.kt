/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.junit.jupiter.api.Test

class MppPaymentsTest {

    private val capability = MppPayments(
        methods = listOf(PaymentMethod.of("tempo"), PaymentMethod.of("stripe")),
        intents = listOf(PaymentIntent.of("charge"))
    )

    @Test
    fun `name is payment`() {
        assertThat(capability.name, equalTo("payment"))
    }

    @Test
    fun `config contains methods and intents`() {
        assertThat(
            capability.config, equalTo(
                mapOf(
                    "methods" to listOf("tempo", "stripe"),
                    "intents" to listOf("charge")
                )
            )
        )
    }

    @Test
    fun `works as McpExtension on ServerMetaData`() {
        val metadata = ServerMetaData("test-server", "1.0")
            .withExtensions(capability)

        assertThat(metadata.capabilities.extensions.containsKey("payment"), equalTo(true))
    }
}
