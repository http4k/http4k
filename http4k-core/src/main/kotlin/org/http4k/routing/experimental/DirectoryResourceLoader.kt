package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.routing.Router
import java.io.ByteArrayInputStream
import java.io.File

typealias DirectoryRenderer = (path: String, filenames: List<String>) -> String

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryRenderer: DirectoryRenderer? = null
) : Router, ResourceLoading {

    override fun match(path: String): HttpHandler? {
        val f = File(baseDir.pathJoin(path))
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(path))
            f.isDirectory -> match(indexFileIn(path)) ?: directoryRenderingHandler(path, f)
            else -> null
        }
    }

    private fun indexFileIn(path: String) = path.pathJoin("index.html")

    private fun directoryRenderingHandler(path: String, dir: File): HttpHandler? =
        directoryRenderer?.invoke(path, dir.list().asList())?.let {
            FakeFileContentsResource(dir, it.toByteArray(Charsets.UTF_8), ContentType.TEXT_HTML)
        }
}

/**
 * Has the last modified etc of a file, but a different content. Used so that the If-Modified-Since handling works
 */
private class FakeFileContentsResource(
    file: File,
    val content: ByteArray,
    contentType: ContentType
) : FileResource(file, contentType) {

    override fun openStream() = ByteArrayInputStream(content)

    override val length = content.size.toLong()

}