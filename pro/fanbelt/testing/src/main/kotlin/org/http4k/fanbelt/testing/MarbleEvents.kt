/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.events.Event
import org.http4k.events.Events
import org.http4k.tracing.TraceCompletion.complete
import org.http4k.tracing.TraceCompletion.incomplete
import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.TraceReporter
import org.http4k.tracing.junit.PrintToConsole
import org.http4k.tracing.junit.RenderingMode
import org.http4k.tracing.junit.ReportingMode
import org.http4k.tracing.junit.TraceNamer
import org.http4k.tracing.junit.TraceNamer.Companion.TestNameAndMethod
import org.http4k.tracing.persistence.FileSystem
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

interface MarbleRenderer {
    fun render(name: String, timeline: Timeline): TraceRender
}

class MarbleEvents(
    private val renderers: List<MarbleRenderer> = listOf(AsciiMarbles(), HtmlMarbles()),
    private val persistence: TraceRenderPersistence = TraceRenderPersistence.FileSystem(File("build/reports/marbles")),
    private val namer: TraceNamer = TestNameAndMethod,
    private val reporter: TraceReporter = TraceReporter.PrintToConsole,
    private val renderingMode: RenderingMode = RenderingMode.Always,
    private val reportingMode: ReportingMode = ReportingMode.OnFailure,
) : Events, AfterTestExecutionCallback {
    private val events = CopyOnWriteArrayList<Pair<Event, Boolean>>()
    private val aliases = mutableMapOf<String, String>()

    @Volatile
    private var recording = false

    @Volatile
    private var recorded = false

    override fun invoke(event: Event) {
        events += event to recording
    }

    fun alias(key: Any, name: String) {
        aliases[key.toString()] = name
    }

    fun <T> record(block: () -> T): T {
        recorded = true
        recording = true
        return try {
            block()
        } finally {
            recording = false
        }
    }

    fun timeline() = Timeline.of(events.filter { !recorded || it.second }.map { it.first }, aliases)

    override fun afterTestExecution(context: ExtensionContext) {
        val completion = if (context.executionException.isPresent) incomplete else complete
        val timeline = timeline()
        if (!renderingMode(completion) || timeline.lanes.isEmpty()) return
        val name = namer(context)
        renderers.forEach { renderer ->
            val render = renderer.render(name, timeline)
            val location = persistence(render)
            if (location != null && reportingMode(completion)) reporter(location, completion, render)
        }
    }
}
