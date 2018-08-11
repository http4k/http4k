package org.http4k.routing.experimental

import java.net.URL
import java.time.Instant

class ClasspathResourceLoader(
    basePackagePath: String,
    private val lastModifiedFinder: (path: String) -> Instant?
) : NewResourceLoader {

    override fun resourceFor(path: String): Resource? {
        val resourcePath = finalBasePath + path
        return javaClass.getResource(resourcePath)?.toResource(lastModifiedFinder(resourcePath))
    }

    private val withStarting = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"
    private val finalBasePath = if (withStarting.endsWith("/")) withStarting else "$withStarting/"
}

private fun URL.toResource(lastModified: Instant?): Resource = URLResource(this, lastModified)