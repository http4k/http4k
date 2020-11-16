package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import java.io.File
import java.time.Instant

internal data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryRenderer: DirectoryRenderer? = null
) : ResourceLoading {

    override fun match(path: String): HttpHandler? = with(File(baseDir.pathJoin(path))) {
        when {
            isFile -> FileResource(this, mimeTypes.forFile(path))
            isDirectory -> match(indexFileIn(path)) ?: directoryRenderer?.let { directoryRenderingHandler(this, it) }
            else -> null
        }
    }

    override  val description = "serving from directory $baseDir"

    private fun indexFileIn(path: String) = path.pathJoin("index.html")

    private fun directoryRenderingHandler(dir: File, renderer: DirectoryRenderer) =
        ResourceListingHandler(
            ResourceSummary(dir.name, Instant.ofEpochMilli(dir.lastModified())),
            dir.listFiles().sortedBy { it.name }.map { ResourceSummary(it.name, Instant.ofEpochMilli(it.lastModified())) },
            renderer)
}
