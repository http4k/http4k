package org.http4k.tracing.junit

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.testing.RecordingEvents
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.StartRendering
import org.http4k.tracing.StopRendering
import org.http4k.tracing.TraceCompletion.complete
import org.http4k.tracing.TraceCompletion.incomplete
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.TraceRenderer
import org.http4k.tracing.TraceReporter
import org.http4k.tracing.Tracer
import org.http4k.tracing.TracerBullet
import org.http4k.tracing.VcrEvents
import org.http4k.tracing.junit.RecordingMode.Auto
import org.http4k.tracing.junit.RecordingMode.Manual
import org.http4k.tracing.junit.RenderingMode.Companion.Always
import org.http4k.tracing.junit.ReportingMode.Companion.OnFailure
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
    private val reporter: TraceReporter = TraceReporter.PrintToConsole,
    private val recordingMode: RecordingMode = Auto,
    private val renderingMode: RenderingMode = Always,
    private val reportingMode: ReportingMode = OnFailure,
) : VcrEvents, AfterTestExecutionCallback {

    private val tracerBullet = TracerBullet(tracers)

    private val events = RecordingEvents().apply {
        if (recordingMode == Manual) this(MetadataEvent(StopRendering))
    }

    override fun afterTestExecution(context: ExtensionContext) {
        val traceCompletion = if (context.executionException.isEmpty) complete else incomplete

        if (renderingMode(traceCompletion)) {
            val traces = tracerBullet(events.toList())

            if (traces.isNotEmpty()) {
                val traceName = traceNamer(context)
                tracePersistence.store(ScenarioTraces(traceName, traces))
                renderers
                    .map { it.render(traceName, traces) }
                    .mapNotNull { traceRenderPersistence(it)?.let { location -> location to it } }
                    .forEach { (location, render) ->
                        if (reportingMode(traceCompletion)) reporter(location, traceCompletion, render)
                    }
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
