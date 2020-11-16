package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.routing.Router
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

object ResourceLoaders {

    object Classpath {
        operator fun invoke(
            basePackagePath: String = "/",
            mimeTypes: MimeTypes = MimeTypes(),
            // We don't want to be grubbing about in jar files for last modified dates, so we default to the creation
            // time of the loader, which make everything out of date every time an app is started, but in date after that.

            // * Must truncate if we are not always to be ahead of the If-Last-Modified header because it doesn't support
            // fractions of seconds.
            constantLastModified: Instant? = Instant.now().truncatedTo(SECONDS),
            lastModifiedFinder: (path: String) -> Instant? = { constantLastModified }
        ): Router = object : ResourceLoading {
            override fun match(path: String): Resource? {
                val resourcePath = basePackagePath.withLeadingSlash().pathJoin(path.orIndexFile())
                return javaClass.getResource(resourcePath)?.toResource(mimeTypes.forFile(resourcePath), lastModifiedFinder(resourcePath))
            }

            override  val description = "serving from classpath at $basePackagePath"
        }

        private fun String.orIndexFile() = if (isEmpty() || endsWith("/")) pathJoin("index.html") else this

        private fun URL.toResource(contentType: ContentType, lastModified: Instant?) = URLResource(this, contentType, lastModified)
    }

    fun Directory(baseDir: String, mimeTypes: MimeTypes = MimeTypes()): Router = DirectoryResourceLoader(baseDir, mimeTypes, null)

    fun ListingDirectory(
        baseDir: String,
        mimeTypes: MimeTypes = MimeTypes(),
        directoryRenderer: DirectoryRenderer = ::simpleDirectoryRenderer
    ): Router = DirectoryResourceLoader(baseDir, mimeTypes, directoryRenderer)
}

/**
 * A little convenience thunk to simplify implementing [Router] for resource loaders.
 */
interface ResourceLoading : Router {

    fun match(path: String): HttpHandler?

    override fun match(request: Request): RouterMatch = when (val matchResult = match(request.uri.path)) {
        is HttpHandler -> MatchingHandler(matchResult)
        else -> Unmatched
    }
}
