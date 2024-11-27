package org.http4k.tracing.persistence

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.bidi_b
import org.http4k.tracing.entire_trace_1
import org.http4k.tracing.fireAndForget_user1
import org.junit.jupiter.api.Test

interface TracePersistenceContract {
    val persistence: TracePersistence

    @Test
    fun `can store and retrieve traces`() {
        with(persistence) {
            store(ScenarioTraces("trace3", listOf(entire_trace_1)))
            store(ScenarioTraces("trace2", listOf(bidi_b)))
            store(ScenarioTraces("trace1", listOf(fireAndForget_user1)))

            assertThat(
                load().toSet(), equalTo(
                    setOf(
                        ScenarioTraces("trace3", listOf(entire_trace_1)),
                        ScenarioTraces("trace2", listOf(bidi_b)),
                        ScenarioTraces("trace1", listOf(fireAndForget_user1))
                    )
                )
            )
        }
    }
}