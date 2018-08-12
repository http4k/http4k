package org.http4k.routing.experimental

import org.http4k.core.MimeTypes
import java.io.File

class DirectoryResourceLoader(
    private val baseDir: String,
    private val mimeTypes: MimeTypes = MimeTypes()
) : NewResourceLoader {

    override fun invoke(path: String): FileResource? {
        val resolved = baseDir.pathJoin(path.orIndexFile())
        val f = File(resolved)
        return if (!f.exists() || !f.isFile) null else FileResource(f, mimeTypes.forFile(resolved))
    }
}