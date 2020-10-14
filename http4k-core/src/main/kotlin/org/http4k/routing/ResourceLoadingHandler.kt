package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.MimeTypes
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

internal class ResourceLoadingHandler(private val pathSegments: String,
                                      private val resourceLoader: ResourceLoader,
                                      extraFileExtensionToContentTypes: Map<String, ContentType>) : HttpHandler {
    private val extMap = MimeTypes(extraFileExtensionToContentTypes)

    override fun invoke(p1: Request): Response = if (p1.uri.path.startsWith(pathSegments)) {
        val path = convertPath(p1.uri.path)
        resourceLoader.load(path)?.let { url ->
            val lookupType = extMap.forFile(path)
            if (p1.method == Method.GET && lookupType != ContentType.OCTET_STREAM) {
                Response(Status.OK)
                    .header("Content-Type", lookupType.value)
                    .body(Body(url.openStream()))
            } else Response(Status.NOT_FOUND)
        } ?: Response(Status.NOT_FOUND)
    } else Response(Status.NOT_FOUND)

    private fun convertPath(path: String): String {
        val newPath = if (pathSegments == "/" || pathSegments == "") path else path.replaceFirst(pathSegments, "")
        val resolved = if (newPath == "/" || newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}
