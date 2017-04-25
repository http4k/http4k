package org.reekwest.http.contract

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.Type.Root
import org.reekwest.http.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Filter
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.then
import org.reekwest.http.core.with
import java.nio.ByteBuffer

class StaticModule(private val basePath: BasePath,
                   private val resourceLoader: ResourceLoader = ResourceLoader.Classpath("/"),
                   private val moduleFilter: Filter = Filter { it }) : org.reekwest.http.contract.Module {
    override fun toRequestRouter(): RequestRouter =
        {
            req ->
            val path = convertPath(BasePath(req.uri.path))
            resourceLoader.load(path)?.let {
                url ->
                if (req.method == GET) {
                    moduleFilter.then({
                        Response(OK)
                            .with(
                                CONTENT_TYPE to ContentType.lookupFor(path),
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
