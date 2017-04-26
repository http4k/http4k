package org.reekwest.http.core

import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import java.nio.ByteBuffer

class StaticContent(private val basePath: String = "", private val resourceLoader: ResourceLoader = ResourceLoader.Classpath()) : HttpHandler {
    override fun invoke(req: Request): Response {
        val path = convertPath(req.uri.path)
        return resourceLoader.load(path)?.let {
            url ->
            if (req.method == GET) {
                Response(OK,
                    listOf("Content-Type" to ContentType.lookupFor(path).value),
                    ByteBuffer.wrap(url.openStream().readBytes())
                )
            } else Response(NOT_FOUND)
        } ?: Response(NOT_FOUND)
    }

    private fun convertPath(path: String): String {
        val newPath = if (basePath == "/" || basePath == "") path else path.replace(basePath, "")
        val resolved = if (newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }

}