package org.http4k.tracing.junit

import org.http4k.tracing.TraceCompletion.complete
import org.http4k.tracing.TraceCompletion.incomplete
import org.http4k.tracing.TraceReporter

val TraceReporter.Companion.PrintToConsole
    get() = TraceReporter { location, completion, render ->
        val message = "Stored $completion ${render.format} trace @ $location"
        when (completion) {
            complete -> println(message)
            incomplete -> System.err.println(message)
        }
    }
