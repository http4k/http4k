/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.events.AutoMarshallingEvents
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.withFanbeltMappings
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class Name private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<Name>(::Name)
}

class TraceJsonTest {
    private val printed = StringBuilder()
    private val sink = AutoMarshallingEvents(ConfigurableMoshi(standardConfig().withFanbeltMappings().value(Name).done())) { printed.appendLine(it) }

    @Test
    fun `traces print as plain json through any json built with the fanbelt mappings, outcomes and tenant included`() {
        sink(eventTenant(TenantId.of("acme"), Executed("reserve", "ann", Changes.of(Topic.of("seats")))))
        sink(eventTenant(TenantId.of("acme"), Executed("reserve", "bob", Rejected(FieldName.of("seat"), "Someone just took F12"))))
        sink(eventTenant(TenantId.of("acme"), Executed("reserve", "eve", Denied)))
        sink(eventTenant(TenantId.of("acme"), Executed("reserve", "mallory", Hidden)))

        assertEquals(
            listOf(
                """{"event":{"operation":"reserve","actor":"ann","outcome":{"type":"Changes","topics":["seats"],"result":{}}},"metadata":{"tenant":"acme"}}""",
                """{"event":{"operation":"reserve","actor":"bob","outcome":{"type":"Rejected","errors":{"seat":"Someone just took F12"}}},"metadata":{"tenant":"acme"}}""",
                """{"event":{"operation":"reserve","actor":"eve","outcome":{"type":"Denied"}},"metadata":{"tenant":"acme"}}""",
                """{"event":{"operation":"reserve","actor":"mallory","outcome":{"type":"Hidden"}},"metadata":{"tenant":"acme"}}""",
            ),
            printed.lines().filter { it.isNotBlank() },
        )
    }
}
