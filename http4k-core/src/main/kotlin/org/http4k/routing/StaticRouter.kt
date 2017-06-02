package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import java.nio.ByteBuffer
import javax.activation.MimetypesFileTypeMap

fun static(resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): Static = Static(resourceLoader, extraPairs.asList().toMap())

class Static internal constructor(internal val resourceLoader: ResourceLoader, internal val extraPairs: Map<String, ContentType>)

infix fun String.by(static: Static): StaticRouter = StaticRouter(this, static.resourceLoader, static.extraPairs)

class StaticRouter(private val basePath: String, private val resourceLoader: ResourceLoader, extraPairs: Map<String, ContentType>) : RoutingHttpHandler {
    override fun match(request: Request): HttpHandler? = invoke(request).let { if (it.status != NOT_FOUND) { _: Request -> it } else null }

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