package org.reekwest.kontrakt

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Filter
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import java.nio.ByteBuffer

class StaticModule(private val basePath: BasePath,
                   private val resourceLoader: ResourceLoader = ResourceLoader.Companion.Classpath("/"),
                   private val moduleFilter: Filter = Filter { it }) : Module {
    override fun toRouter(): Router =
        {
            req ->
            val path = convertPath(BasePath(req.uri.path))
            resourceLoader.load(path)?.let {
                url ->
                if (req.method == GET) {
                    moduleFilter.then({
                        Response(OK)
                            .with(
                                Header.Common.CONTENT_TYPE to ContentType.lookupFor(path),
                                Body.required() to ByteBuffer.wrap(url.openStream().readBytes())
                            )
                    })
                } else null
            }
        }

    private fun convertPath(path: BasePath): String {
        val newPath = if (basePath == Root) path.toString() else path.toString().replace(basePath.toString(), "")
        val resolved = if (newPath.isBlank()) "/index.html" else newPath
        return resolved.replaceFirst("/", "")
    }
}
