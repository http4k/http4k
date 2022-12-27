package org.http4k.tracing.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.events.Event
import org.http4k.events.EventFilters.AddZipkinTraces
import org.http4k.events.MetadataEvent
import org.http4k.events.then
import org.http4k.tracing.Actor
import org.http4k.tracing.ActorType.System
import org.http4k.tracing.FireAndForget
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceStep
import org.http4k.tracing.Tracer
import org.http4k.tracing.junit.RecordingMode.Auto
import org.http4k.tracing.junit.RecordingMode.Manual
import org.http4k.tracing.persistence.InMemory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Proxy
import java.util.Optional

class TracerBulletEventsTest {
    private val traceRenderPersistence = TraceRenderPersistence.InMemory()
    private val tracePersistence = TracePersistence.InMemory()
    private val title = "Title (variant): toString"

    @Test
    fun `write traces`() {
        val events = tracerBulletEvents(Auto)

        val eventsToSend = listOf(MyEvent, MyOtherEvent, YetAnotherEvent)
        eventsToSend.forEach(AddZipkinTraces().then(events))

        events.afterTestExecution(FakeEC())

        val traces = eventsToSend.map(::toTrace)
        assertThat(
            traceRenderPersistence.toList(),
            equalTo(listOf(TraceRender(title, title, traces.toString())))
        )
        assertThat(
            tracePersistence.load().toList(),
            equalTo(listOf(ScenarioTraces(title, traces)))
        )
    }

    @Test
    fun `does not write empty traces`() {
        val events = tracerBulletEvents(Auto)
        events.afterTestExecution(FakeEC())

        assertThat(tracePersistence.load().toList(), equalTo(listOf()))
        assertThat(traceRenderPersistence.toList(), equalTo(listOf()))
    }

    @Test
    fun `does not write empty traces when rendering off`() {
        val events = tracerBulletEvents(Manual)

        val eventsToSend = listOf(MyEvent, MyOtherEvent, YetAnotherEvent)

        eventsToSend.forEach(AddZipkinTraces().then(events))

        events.afterTestExecution(FakeEC())

        assertThat(tracePersistence.load().toList(), equalTo(listOf()))
        assertThat(traceRenderPersistence.toList(), equalTo(listOf()))
    }

    @Test
    fun `can enable and disable rendering`() {
        val events = tracerBulletEvents(Manual)

        val decoratedEvents = AddZipkinTraces().then(events)
        decoratedEvents(MyEvent)

        events.record { decoratedEvents(MyOtherEvent) }

        decoratedEvents(MyEvent)

        events.record { decoratedEvents(YetAnotherEvent) }

        events.afterTestExecution(FakeEC())

        val traces = listOf(toTrace(MyOtherEvent), toTrace(YetAnotherEvent))

        assertThat(
            traceRenderPersistence.toList(),
            equalTo(listOf(TraceRender(title, title, traces.toString())))
        )
        assertThat(
            tracePersistence.load().toList(), equalTo(
                listOf(ScenarioTraces(title, traces))
            )
        )
    }

    private fun tracerBulletEvents(recordingMode: RecordingMode) = TracerBulletEvents(
        listOf(MyTracer()),
        listOf(MyTraceRenderer()),
        traceRenderPersistence,
        { title },
        tracePersistence,
        recordingMode
    )
}

private class MyTraceRenderer : TraceRenderer {
    override fun render(scenarioName: String, steps: List<TraceStep>): TraceRender =
        TraceRender(scenarioName, scenarioName, steps.toString())
}

private class MyTracer : Tracer {
    override fun invoke(parent: MetadataEvent, rest: List<MetadataEvent>, tracer: Tracer) =
        listOf(toTrace(parent.event))
}

object MyEvent : Event
object MyOtherEvent : Event
object YetAnotherEvent : Event

inline fun <reified T> proxy(): T = Proxy.newProxyInstance(
    T::class.java.classLoader,
    arrayOf(T::class.java)
) { _, m, _ -> TODO(m.name + " not implemented") } as T

private class FakeEC : ExtensionContext by proxy() {
    override fun getExecutionException() = Optional.empty<Throwable>()
    override fun getTestMethod() = Optional.of(String::class.java.getMethod("toString"))
}

private fun toTrace(event: Event): FireAndForget {
    val name = event.javaClass.simpleName
    return FireAndForget(
        Actor(name, System),
        Actor("target", System),
        "req",
        emptyList()
    )
}
