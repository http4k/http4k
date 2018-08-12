package org.http4k.routing.experimental

import org.http4k.core.ContentType
import org.http4k.core.MimeTypes
import java.net.URL
import java.time.Instant

class ClasspathResourceLoader(
    basePackagePath: String,
    private val mimeTypes: MimeTypes = MimeTypes(),
    private val lastModifiedFinder: (path: String) -> Instant?
) : NewResourceLoader {

    override fun invoke(path: String): Resource? {
        val resourcePath = finalBasePath.pathJoin(path.orIndexFile())
        return javaClass.getResource(resourcePath)?.toResource(mimeTypes.forFile(resourcePath), lastModifiedFinder(resourcePath))
    }

    private val finalBasePath = if (basePackagePath.startsWith("/")) basePackagePath else "/$basePackagePath"
}

private fun URL.toResource(contentType: ContentType, lastModified: Instant?) = URLResource(this, contentType, lastModified)