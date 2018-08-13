package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Uri.Companion.of
import org.http4k.routing.Router
import java.io.ByteArrayInputStream
import java.io.File

typealias DirectoryRenderer = (request: Request, filenames: List<String>) -> String

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryRenderer: DirectoryRenderer? = null
) : Router {

    override fun match(request: Request): HttpHandler? {
        val path = request.uri.path
        val f = File(baseDir.pathJoin(path))
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(path))
            f.isDirectory -> match(request.uri(indexFileIn(path))) ?: directoryRenderingHandler(f, request)
            else -> null
        }
    }

    private fun indexFileIn(path: String) = of(path.pathJoin("index.html"))

    private fun directoryRenderingHandler(dir: File, request: Request): HttpHandler? =
        directoryRenderer?.invoke(request, dir.list().asList())?.let {
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