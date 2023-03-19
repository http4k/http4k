package org.http4k.tracing.persistence

import org.http4k.tracing.TraceLoader
import org.http4k.tracing.TracePersistence
import java.io.File

fun TraceLoader.Companion.DirectoryTree(start: File) = object : TraceLoader {
    override fun load() = start.walkTopDown().filter { it.isDirectory }
        .flatMap { TracePersistence.FileSystem(it).load() }.toList().sortedBy { it.name }
}
