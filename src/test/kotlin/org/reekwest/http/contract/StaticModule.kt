package org.reekwest.http.contract

import org.reekwest.http.core.*

class StaticModule(private val basePath: BasePath,
                   private val resourceLoader: org.reekwest.http.contract.ResourceLoader,
                   private val moduleFilter: org.reekwest.http.core.Filter = org.reekwest.http.core.Filter { it }) : org.reekwest.http.contract.Module {
    override fun toRequestRouter(): org.reekwest.http.contract.RequestRouter =
        {
            req ->
            val path = convertPath(org.reekwest.http.contract.BasePath(req.uri.path))
            resourceLoader.load(path)?.let {
                if (req.method == org.reekwest.http.core.Method.GET) {
                    moduleFilter.then({
                        org.reekwest.http.core.Response(org.reekwest.http.core.Status.Companion.OK)
                            .with(org.reekwest.http.contract.Header.Common.CONTENT_TYPE to org.reekwest.http.core.ContentType.Companion.lookupFor(path)
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
