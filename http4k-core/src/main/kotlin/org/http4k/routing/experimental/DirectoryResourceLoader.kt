package org.http4k.routing.experimental

import java.io.File

class DirectoryResourceLoader(val baseDir: String) : NewResourceLoader {

    override fun resourceFor(path: String) = File(baseDir, path).let { f ->
        if (!f.exists() || !f.isFile) null else FileResource(f)
    }
}