package org.http4k.tracing.persistence

import org.http4k.tracing.TraceRenderPersistence
import java.io.File
import java.nio.file.Files
import java.util.Locale

/**
 * Writes trace renders to files in a known directory location.
 */
fun TraceRenderPersistence.Companion.FileSystem(dir: File = Files.createTempDirectory("").toFile()) =
    TraceRenderPersistence {
        File(dir.apply { mkdirs() }, "${it.title}.${it.format.lowercase(Locale.getDefault())}").writeText(it.content)
    }

