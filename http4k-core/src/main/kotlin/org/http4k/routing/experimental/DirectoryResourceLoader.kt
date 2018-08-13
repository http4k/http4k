package org.http4k.routing.experimental

import org.http4k.core.*
import org.http4k.core.Uri.Companion.of
import org.http4k.routing.Router
import java.io.ByteArrayInputStream
import java.io.File

typealias DirectoryListingRenderer = (base: Uri, filenames: List<String>) -> String

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryListingRenderer: DirectoryListingRenderer? = null
) : Router {

    override fun match(request: Request): HttpHandler? {
        val path = request.uri.path
        val resolved = baseDir.pathJoin(path)
        val f = File(resolved)
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(resolved))
            f.isDirectory -> match(request.uri(of(path.pathJoin("index.html")))) ?: directoryRenderingHandler(f, request.uri)
            else -> null
        }
    }

    private fun directoryRenderingHandler(dir: File, uri: Uri): HttpHandler? =
        directoryListingRenderer?.invoke(uri, dir.list().asList())?.let {
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