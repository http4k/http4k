package org.http4k.tracing.junit

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.testing.RecordingEvents
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.StartRendering
import org.http4k.tracing.StopRendering
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.Tracer
import org.http4k.tracing.TracerBullet
import org.http4k.tracing.VcrEvents
import org.http4k.tracing.junit.RecordingMode.Auto
import org.http4k.tracing.junit.RecordingMode.Manual
import org.http4k.tracing.junit.RenderingMode.Always
import org.http4k.tracing.junit.TraceNamer.Companion.TestNameAndMethod
import org.http4k.tracing.persistence.InMemory
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit plugin which is also an Events implementation that generates Trace renderings and stores them.
 */
open class TracerBulletEvents(
    tracers: List<Tracer>,
    private val renderers: List<TraceRenderer>,
    private val traceRenderPersistence: TraceRenderPersistence,
    private val traceNamer: TraceNamer = TestNameAndMethod,
    private val tracePersistence: TracePersistence = TracePersistence.InMemory(),
    private val recordingMode: RecordingMode = Auto,
    private val renderingMode: RenderingMode = Always,
    ) : VcrEvents, AfterTestExecutionCallback {

    private val tracerBullet = TracerBullet(tracers)

    private val events = RecordingEvents().apply {
        if (recordingMode == Manual) this(MetadataEvent(StopRendering))
    }

    override fun afterTestExecution(context: ExtensionContext) {
        if (renderingMode.shouldRender(context)) {
            val traces = tracerBullet(events.toList())
            if(traces.isNotEmpty()) {
                val traceName = traceNamer(context)
                tracePersistence.store(ScenarioTraces(traceName, traces))
                renderers.forEach { traceRenderPersistence(it.render(traceName, traces)) }
            }
        }
    }

    override fun <T> record(block: () -> T): T {
        events(MetadataEvent(StartRendering))
        return block().also {
            events(MetadataEvent(StopRendering))
        }
    }

    override fun <T> pause(block: () -> T): T {
        events(MetadataEvent(StopRendering))
        return block().also {
            events(MetadataEvent(StartRendering))
        }
    }

    override fun toString() = events.toString()

    override fun invoke(p1: Event) = events(p1)

    override fun iterator() = events
        .map { if (it is MetadataEvent) it.event else it }
        .iterator()
}
