package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.routing.Router
import java.io.File
import java.time.Instant

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryRenderer: DirectoryRenderer? = null
) : Router, ResourceLoading {

    override fun match(path: String): HttpHandler? {
        val f = File(baseDir.pathJoin(path))
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(path))
            f.isDirectory -> match(indexFileIn(path)) ?: directoryRenderer?.let { directoryRenderingHandler(f, it) }
            else -> null
        }
    }

    private fun indexFileIn(path: String) = path.pathJoin("index.html")

}

private fun directoryRenderingHandler(dir: File, renderer: DirectoryRenderer) =
    ResourceListingHandler(
        ResourceSummary(dir.name, Instant.ofEpochMilli(dir.lastModified())),
        dir.listFiles().map { ResourceSummary(it.name, Instant.ofEpochMilli(it.lastModified())) },
        renderer)