package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.feature.Changes
import org.http4k.tracing.TraceCompletion
import org.http4k.tracing.TraceCompletion.incomplete
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.persistence.InMemory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Proxy
import java.util.Optional

class MarbleEventsTest {
    private val persistence = TraceRenderPersistence.InMemory()
    private val reported = mutableListOf<Pair<Uri, TraceCompletion>>()

    private fun marbles(persistence: TraceRenderPersistence = this.persistence) = MarbleEvents(
        renderers = listOf(AsciiMarbles()),
        persistence = persistence,
        namer = { "Seats - two fans" },
        reporter = { location, completion, _ -> reported += location to completion },
    )

    @Test
    fun `after a test, each renderer's marbles are stored under the test's name`() {
        val marbles = marbles()
        marbles.alias("f00d", "Midnight Owls")
        marbles(Executed("reserve", "ann", Changes.of(setOf(Topic.of("bookings:f00d")))))
        marbles(Published(Topic.of("bookings:f00d")))

        marbles.afterTestExecution(test())

        val render = persistence.single()
        assertThat(render.title to render.format, equalTo("Seats - two fans" to "MARBLES"))
        assertThat(render.content, equalTo("""
            ann reserve             -✓--
            bookings:Midnight Owls  ---●
        """.trimIndent()))
    }

    @Test
    fun `a test that traced nothing stores nothing`() {
        marbles().afterTestExecution(test())

        assertThat(persistence.toList(), equalTo(emptyList<TraceRender>()))
    }

    @Test
    fun `once a test records, only what happened while recording is drawn`() {
        val marbles = marbles()
        marbles(Executed("watch", "ann", Changes.of(emptySet())))

        marbles.record { marbles(Executed("reserve", "ann", Changes.of(emptySet()))) }
        marbles(Executed("leave", "ann", Changes.of(emptySet())))

        assertThat(marbles.timeline().lanes.map { it.name }, equalTo(listOf("ann reserve")))
    }

    @Test
    fun `where a render was stored is reported when the test fails`() {
        val stored = TraceRenderPersistence { Uri.of("file:///marbles.txt") }
        val marbles = marbles(persistence = stored)
        marbles(Executed("reserve", "ann", Changes.of(emptySet())))

        marbles.afterTestExecution(test())
        marbles.afterTestExecution(test(failed = true))

        assertThat(reported, equalTo(listOf(Uri.of("file:///marbles.txt") to incomplete)))
    }

    private fun test(failed: Boolean = false) = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ExtensionContext::class.java)) { _, method, _ ->
        when (method.name) {
            "getExecutionException" -> if (failed) Optional.of(AssertionError()) else Optional.empty<Throwable>()
            else -> throw UnsupportedOperationException(method.name)
        }
    } as ExtensionContext
}
