package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.RouteMatcher
import org.http4k.routing.Router
import org.http4k.routing.RouterDescription.Companion.unavailable
import org.http4k.routing.RoutingMatch
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

object ResourceLoaders {

    class Classpath(
        private val basePackagePath: String = "/",
        private val mimeTypes: MimeTypes = MimeTypes(),
        // We don't want to be grubbing about in jar files for last modified dates, so we default to the creation
        // time of the loader, which make everything out of date every time an app is started, but in date after that.

        // * Must truncate if we are not always to be ahead of the If-Last-Modified header because it doesn't support
        // fractions of seconds.
        private val constantLastModified: Instant? = Instant.now().truncatedTo(SECONDS),
        private val lastModifiedFinder: (path: String) -> Instant? = { constantLastModified }
    ) : RouteMatcher<Response, Filter> {
        override fun match(request: Request): RoutingMatch<Response> {
            val resourcePath = basePackagePath.withLeadingSlash().pathJoin(request.uri.path.orIndexFile())
            return when (val resource = javaClass.getResource(resourcePath)) {
                null -> RoutingMatch(2, unavailable, { _: Request -> Response(Status.NOT_FOUND) })
                else -> RoutingMatch(
                    0, unavailable, resource.toResource(
                        mimeTypes.forFile(resourcePath),
                        lastModifiedFinder(resourcePath)
                    )
                )
            }
        }

        override fun withBasePath(prefix: String): RouteMatcher<Response, Filter> = this

        override fun withRouter(other: Router): RouteMatcher<Response, Filter> = this

        override fun withFilter(new: Filter): RouteMatcher<Response, Filter> = this
    }

    private fun String.orIndexFile() = if (isEmpty() || endsWith("/")) pathJoin("index.html") else this

    private fun URL.toResource(contentType: ContentType, lastModified: Instant?) =
        URLResource(this, contentType, lastModified)

    fun Directory(baseDir: String, mimeTypes: MimeTypes = MimeTypes()): RouteMatcher<Response, Filter> =
        DirectoryResourceLoader(baseDir, mimeTypes, null)

    fun ListingDirectory(
        baseDir: String,
        mimeTypes: MimeTypes = MimeTypes(),
        directoryRenderer: DirectoryRenderer = ::simpleDirectoryRenderer
    ): RouteMatcher<Response, Filter> = DirectoryResourceLoader(baseDir, mimeTypes, directoryRenderer)
}
