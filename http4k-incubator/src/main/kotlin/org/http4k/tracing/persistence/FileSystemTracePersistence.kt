package org.http4k.tracing.persistence

import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.persistence.TraceMoshi.asA
import org.http4k.tracing.persistence.TraceMoshi.asFormatString
import org.http4k.tracing.persistence.TraceMoshi.prettify
import java.io.File
import java.nio.file.Files.createTempDirectory

/**
 * Records all traces to JSON format in a known directory location.
 */
fun TracePersistence.Companion.FileSystem(dir: File = createTempDirectory("").toFile()) = object : TracePersistence {
    override fun store(trace: ScenarioTraces) {
        File(dir.apply { mkdirs() }, trace.name + TRACE_SUFFIX)
            .writeText(prettify(asFormatString(trace)))
    }

    override fun load() = dir.list()
        ?.filter { it.endsWith(TRACE_SUFFIX) }
        ?.map { asA<ScenarioTraces>(File(dir, it).readText()) }
        ?: emptyList()

    private val TRACE_SUFFIX = ".trace.json"
}

