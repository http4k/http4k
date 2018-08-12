package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.intellij.lang.annotations.Language
import java.io.ByteArrayInputStream
import java.io.File

// Proof of concept to show how this could be done

fun directoryListing(
    renderer: (base: Uri, filenames: List<String>) -> String = ::simpleDirectoryRenderer
): (dir: File) -> HttpHandler = { dir ->
    directoryListing(dir, renderer)
}


private fun directoryListing(
    dir: File,
    renderer: (base: Uri, filenames: List<String>) -> String
): HttpHandler = { request ->
    val content = renderer(request.uri, dir.listFiles().map(File::getName)).toByteArray(Charsets.UTF_8)
    FakeFileContentsResource(dir, content, ContentType.TEXT_HTML).invoke(request)
}

@Language("HTML")
fun simpleDirectoryRenderer(base: Uri, filenames: List<String>) =
    """
    <html>
        <body>
            <h1>$base</h1>
            <ol>
                ${listOfFiles(base, filenames)}
            <ol>
        </body>
    </html>
    """.trimIndent()

private fun listOfFiles(base: Uri, filenames: List<String>): String =
    filenames.joinToString(separator = "\n") { name ->
        """<li><a href="${hrefFor(base, name)}">$name</a></li>"""
    }

private fun hrefFor(base: Uri, filename: String) = Uri.of(base.toString().pathJoin(filename))

/**
 * Has the last modified etc of a file, but a different content
 */
class FakeFileContentsResource(
    file: File,
    val content: ByteArray,
    contentType: ContentType
) : FileResource(file, contentType) {

    override fun openStream() = ByteArrayInputStream(content)

    override val length = content.size.toLong()

}