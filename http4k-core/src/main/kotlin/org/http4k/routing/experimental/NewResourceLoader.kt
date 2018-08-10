package org.http4k.routing.experimental

import java.io.File
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit


interface NewResourceLoader {

    fun resourceFor(path: String): Resource?

    companion object {

        fun Classpath(
            basePackagePath: String = "/",
            constantLastModified: Instant? = Instant.now().truncatedTo(ChronoUnit.SECONDS), // see * below,
            lastModifiedFinder: (path: String) -> Instant? = { constantLastModified }
        ): ClasspathResourceLoader {
            // We don't want to be grubbing about in jar files for last modified dates, so we default to the creation
            // time of the loader, which make everything out of date every time an app is started, but in date after that.

            // * Must truncate if we are not always to be ahead of the If-Last-Modified header because it doesn't support
            // fractions of seconds.
            return ClasspathResourceLoader(basePackagePath, lastModifiedFinder)
        }

        fun Directory(baseDir: String) = object : NewResourceLoader {

            override fun resourceFor(path: String) = File(finalBaseDir, path).let { f ->
                if (!f.exists() || !f.isFile) null else FileResource(f)
            }

            private val finalBaseDir = if (baseDir.endsWith("/")) baseDir else "$baseDir/"
        }
    }
}

class ClasspathResourceLoader(
    basePackagePath: String,
    private val lastModifiedFinder: (path: String) -> Instant?
) : NewResourceLoader {

    override fun resourceFor(path: String): Resource? {
        val resourcePath = finalBasePath + path
        return javaClass.getResource(resourcePath)?.toResource(lastModifiedFinder(resourcePath))
    }

    private val withStarting = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"
    private val finalBasePath = if (withStarting.endsWith("/")) withStarting else "$withStarting/"
}

private fun URL.toResource(lastModified: Instant?): Resource = URLResource(this, lastModified)

