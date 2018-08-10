package org.http4k.routing.experimental

import org.http4k.core.*
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.RoutingHttpHandler

internal class NewResourceLoadingHandler(private val pathSegments: String,
                                         private val resourceLoader: NewResourceLoader,
                                         extraPairs: Map<String, ContentType>) : HttpHandler {
    private val extMap = MimeTypes(extraPairs)

    override fun invoke(request: Request): Response =
        if (request.uri.path.startsWith(pathSegments)) {
            val path = convertPath(request.uri.path)
            resourceLoader.resourceFor(path)?.let { resource ->
                val lookupType = extMap.forFile(path)
                if (request.method == GET && lookupType != OCTET_STREAM) {
                    resource.invoke(request).header("Content-Type", lookupType.value)
                } else Response(NOT_FOUND)
            } ?: Response(NOT_FOUND)
        } else Response(NOT_FOUND)

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replace(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}


internal data class NewStaticRoutingHttpHandler(private val pathSegments: String,
                                                private val resourceLoader: NewResourceLoader,
                                                private val extraPairs: Map<String, ContentType>,
                                                private val filter: Filter = Filter.NoOp
) : RoutingHttpHandler {

    override fun withFilter(new: Filter): RoutingHttpHandler = copy(filter = new.then(filter))

    override fun withBasePath(new: String): RoutingHttpHandler = copy(pathSegments = new + pathSegments)

    private val handlerNoFilter = NewResourceLoadingHandler(pathSegments, resourceLoader, extraPairs)
    private val handlerWithFilter = filter.then(handlerNoFilter)

    override fun match(request: Request): HttpHandler? = handlerNoFilter(request).let {
        if (it.status != NOT_FOUND) filter.then { _: Request -> it } else null
    }

    override fun invoke(request: Request): Response = handlerWithFilter(request)
}

fun static(resourceLoader: NewResourceLoader, vararg extraPairs: Pair<String, ContentType>): RoutingHttpHandler =
    NewStaticRoutingHttpHandler("", resourceLoader, extraPairs.asList().toMap())