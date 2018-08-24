package org.http4k.routing.experimental

import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.routing.Router
import java.time.Instant
import java.time.temporal.ChronoUnit

object ResourceLoaders {

    fun Classpath(
        basePackagePath: String = "/",
        mimeTypes: MimeTypes = MimeTypes(),
        constantLastModified: Instant? = Instant.now().truncatedTo(ChronoUnit.SECONDS), // see * below,
        lastModifiedFinder: (path: String) -> Instant? = { constantLastModified }
    ): ClasspathResourceLoader {
        // We don't want to be grubbing about in jar files for last modified dates, so we default to the creation
        // time of the loader, which make everything out of date every time an app is started, but in date after that.

        // * Must truncate if we are not always to be ahead of the If-Last-Modified header because it doesn't support
        // fractions of seconds.
        return ClasspathResourceLoader(basePackagePath, mimeTypes, lastModifiedFinder)
    }

    fun Directory(
        baseDir: String,
        mimeTypes: MimeTypes = MimeTypes()
    ) = DirectoryResourceLoader(baseDir, mimeTypes, null)

    fun ListingDirectory(
        baseDir: String,
        mimeTypes: MimeTypes = MimeTypes(),
        directoryRenderer: DirectoryRenderer = ::simpleDirectoryRenderer
    ) = DirectoryResourceLoader(baseDir, mimeTypes, directoryRenderer)
}

/**
 * A little convenience thunk to simplify implementing [Router] for resource loaders.
 */
interface ResourceLoading : Router {

    fun match(path: String): HttpHandler?

    override fun match(request: Request): HttpHandler? = match(request.uri.path)
}
