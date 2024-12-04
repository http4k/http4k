package org.http4k.routing.experimental

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.HttpMatchResult
import org.http4k.routing.RouteMatcher
import org.http4k.routing.Router
import java.io.File
import java.time.Instant

internal data class DirectoryResourceLoader(
    val baseDir: String,
    val mimeTypes: MimeTypes = MimeTypes(),
    val directoryRenderer: DirectoryRenderer? = null
) : RouteMatcher {

    override fun match(request: Request): HttpMatchResult =
        when (val match = match(request.uri.path)) {
            is HttpHandler -> HttpMatchResult(0, match)
            else -> HttpMatchResult(2, { Response(NOT_FOUND) })
        }

    private fun match(path: String): HttpHandler? = with(File(baseDir.pathJoin(path))) {
        when {
            !isUnder(baseDir) -> null
            isFile -> FileResource(this, mimeTypes.forFile(path))
            isDirectory -> match(indexFileIn(path)) ?: directoryRenderer?.let {
                directoryRenderingHandler(
                    this,
                    it
                )
            }

            else -> null
        }
    }

    private fun File.isUnder(baseDir: String) = canonicalPath.startsWith(File(baseDir).canonicalPath)

    private fun indexFileIn(path: String) = path.pathJoin("index.html")

    private fun directoryRenderingHandler(dir: File, renderer: DirectoryRenderer) =
        ResourceListingHandler(
            ResourceSummary(dir.name, Instant.ofEpochMilli(dir.lastModified())),
            dir.listFiles()?.sortedBy { it.name }
                ?.map { ResourceSummary(it.name, Instant.ofEpochMilli(it.lastModified())) }
                ?: emptyList(),
            renderer
        )

    override fun withBasePath(prefix: String) = this

    override fun withRouter(other: Router) = this

    override fun withFilter(new: Filter) = this
}
