package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import java.io.File

data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryLister: ((File) -> HttpHandler)? = null
) : NewResourceLoader {

    override fun invoke(path: String): HttpHandler? {
        val resolved = baseDir.pathJoin(path)
        val f = File(resolved)
        return when {
            f.isFile -> FileResource(f, mimeTypes.forFile(resolved))
            f.isDirectory -> invoke(path.pathJoin("index.html")) ?: directoryLister?.invoke(f)
            else -> null
        }
    }
}