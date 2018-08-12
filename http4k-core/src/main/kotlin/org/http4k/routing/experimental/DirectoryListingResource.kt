package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.io.ByteArrayInputStream
import java.io.File

// Proof of concept to show how this could be done
// Extends FileResource so that last-modified handing is reused
class DirectoryListingResource(dir: File, private val uri: Uri) : FileResource(dir, ContentType.TEXT_HTML) {

    companion object : (File) -> HttpHandler {
        override fun invoke(file: File): HttpHandler = { request -> DirectoryListingResource(file, request.uri)(request) }
    }

    init {
        check(dir.isDirectory) { "$dir is not a directory" }
    }

    private val listing = directoryListing(dir.listFiles())

    override val length: Long = listing.length.toLong()

    override fun openStream() = ByteArrayInputStream(listing.toByteArray(Charsets.UTF_8))

    private fun directoryListing(files: Array<out File>) =
        files.joinToString(prefix = "<ol>\n", separator = "\n", postfix = "\n</ol>") { file ->
            """<li><a href="${hrefFor(file)}">${file.name}</a></li>"""
        }

    private fun hrefFor(file: File) = Uri.of(uri.toString().pathJoin(file.name))
}