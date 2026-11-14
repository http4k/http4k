/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.feature

import org.http4k.fanbelt.events.EventsJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperationRefusedJsonTest {
    @Test
    fun `a refusal is written as the operation, the rule and which refusal, and reads back`() {
        val refused = OperationRefused("knock", "enterable", Outcome.Hidden)
        val json = """{"operation":"knock","rule":"enterable","refusal":"Hidden"}"""

        assertEquals(json, EventsJson.asFormatString(refused))
        assertEquals(refused, EventsJson.asA<OperationRefused>(json))
        assertEquals(Outcome.Denied, EventsJson.asA<OperationRefused>(json.replace("Hidden", "Denied")).refusal)
    }
}
