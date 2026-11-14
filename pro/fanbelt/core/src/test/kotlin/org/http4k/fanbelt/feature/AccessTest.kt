/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.input.FieldName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccessTest {
    private val refusals: List<Access<String>> = listOf(Hidden, Denied, Rejected(FieldName.of("room"), "That room has gone"))

    @Test
    fun `map changes what an allowed answer found, and leaves a refusal as it is`() {
        assertEquals(Allowed(3), Allowed("kitchen").map { it.length - 4 })
        refusals.forEach { assertEquals(it, it.map { found -> found.length }) }
    }

    @Test
    fun `flatMap carries on from an allowed answer, which may refuse, and leaves a refusal as it is`() {
        assertEquals(Allowed(7), Allowed("kitchen").flatMap { Allowed(it.length) })
        assertEquals(Denied, Allowed("kitchen").flatMap { Denied })
        refusals.forEach { assertEquals(it, it.flatMap { found -> Allowed(found.length) }) }
    }
}
