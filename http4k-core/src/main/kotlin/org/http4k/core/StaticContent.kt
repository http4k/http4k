package org.http4k.core

import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import java.nio.ByteBuffer
import javax.activation.MimetypesFileTypeMap

class StaticContent(private val basePath: String = "", private val resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>) : HttpHandler {

    private val extMap = MimetypesFileTypeMap(ContentType::class.java.getResourceAsStream("/META-INF/mime.types"))

    init {
        extMap.addMimeTypes(extraPairs
            .map { (first, second) -> second.value + "\t\t\t" + first }.joinToString("\n")
        )
    }

    override fun invoke(req: Request): Response {
        val path = convertPath(req.uri.path)
        return resourceLoader.load(path)?.let {
            url ->
            val lookupType = ContentType(extMap.getContentType(path))
            if (req.method == GET && lookupType != OCTET_STREAM) {
                Response(OK)
                    .header("Content-Type", lookupType.value)
                    .body(Body(ByteBuffer.wrap(url.openStream().readBytes())))
            } else Response(NOT_FOUND)
        } ?: Response(NOT_FOUND)
    }

    private fun convertPath(path: String): String {
        val newPath = if (basePath == "/" || basePath == "") path else path.replace(basePath, "")
        val resolved = if (newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }

}