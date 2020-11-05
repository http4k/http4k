package org.http4k.filter

import org.http4k.aws.MultipartS3Upload
import org.http4k.core.BodyMode
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.then

fun ClientFilters.ChunkKeyContentsIfRequired(requestBodyMode: BodyMode, size: Int = 5 * 1024 * 1024): Filter {
    val upload = MultipartS3Upload(size, requestBodyMode)
    return Filter { next ->
        HttpHandler {
            if (it.method == Method.PUT && it.uri.path.trimEnd('/').isNotBlank()) upload.then(next)(it) else next(it)
        }
    }
}
