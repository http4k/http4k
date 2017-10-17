package org.http4k.filter

import org.http4k.aws.MultipartS3Upload
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.then

fun ClientFilters.ChunkKeyContentsIfRequired(size: Int = 5 * 1024 * 1024): Filter {
    val chunker = MultipartS3Upload(size)
    return Filter { next ->
        {
            if (it.method == Method.PUT && it.uri.path.trimEnd('/').isNotBlank()) chunker.then(next)(it) else next(it)
        }
    }
}
