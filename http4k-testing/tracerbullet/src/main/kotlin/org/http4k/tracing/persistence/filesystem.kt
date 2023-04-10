package org.http4k.tracing.persistence

import org.http4k.core.Uri
import org.http4k.tracing.ScenarioTraces
import org.http4k.tracing.TraceLoader
import org.http4k.tracing.TracePersistence
import org.http4k.tracing.TraceRenderPersistence
import org.http4k.tracing.persistence.TraceMoshi.asA
import org.http4k.tracing.persistence.TraceMoshi.asFormatString
import org.http4k.tracing.persistence.TraceMoshi.prettify
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.util.Locale

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

/**
 * Writes trace renders to files in a known directory location.
 */
fun TraceRenderPersistence.Companion.FileSystem(
    dir: File = createTempDirectory("").toFile()
) = TraceRenderPersistence {
    val file = File(dir.apply { mkdirs() }, "${it.title}.${it.format.lowercase(Locale.getDefault())}")
    file.writeText(it.content)
    Uri.of("file:////" + file.absolutePath.replace(" ", "%20"))
}

/**
 * Reads trace renders by walking a directory tree
 */
fun TraceLoader.Companion.DirectoryTree(start: File) = object : TraceLoader {
    override fun load() = start.walkTopDown().filter { it.isDirectory }
        .flatMap { TracePersistence.FileSystem(it).load() }.toList().sortedBy { it.name }
}
