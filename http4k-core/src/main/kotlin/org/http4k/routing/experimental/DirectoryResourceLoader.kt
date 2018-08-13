package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Uri.Companion.of
import org.http4k.routing.Router
import java.io.File

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryLister: ((File) -> HttpHandler)? = null
) : Router {

    override fun match(request: Request): HttpHandler? {
        val path = request.uri.path
        val resolved = baseDir.pathJoin(path)
        val f = File(resolved)
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(resolved))
            f.isDirectory -> match(request.uri(of(path.pathJoin("index.html")))) ?: directoryLister?.invoke(f)
            else -> null
        }
    }
}