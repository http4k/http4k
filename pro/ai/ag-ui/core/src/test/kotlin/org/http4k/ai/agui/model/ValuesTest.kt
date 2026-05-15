/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ValuesTest {

    @Test
    fun `ThreadId roundtrips through value`() {
        assertThat(ThreadId.of("thread-1").value, equalTo("thread-1"))
    }

    @Test
    fun `RunId random is well-formed UUID`() {
        assertThat(RunId.random().value.length, equalTo(36))
    }

    @Test
    fun `MessageId is non-blank`() {
        assertThat(MessageId.of("msg-1").value, equalTo("msg-1"))
    }

    @Test
    fun `ToolCallId is non-blank`() {
        assertThat(ToolCallId.of("call-1").value, equalTo("call-1"))
    }

    @Test
    fun `StepName is non-blank`() {
        assertThat(StepName.of("planning").value, equalTo("planning"))
    }

    @Test
    fun `ActivityType is non-blank`() {
        assertThat(ActivityType.of("thinking").value, equalTo("thinking"))
    }
}
