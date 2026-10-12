package org.http4k.core

import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import org.http4k.core.ContentEncodingName.Companion.ZSTD

/**
 * Zstandard content-encoding. Level ranges from 1 (fastest) to 22 (smallest).
 */
fun ContentEncoding.Companion.Zstd(level: Int = 3) = ContentEncoding(
    ZSTD,
    { ZstdOutputStream(it).setLevel(level) },
    { ZstdInputStream(it).setLongMax(MAX_HTTP_WINDOW_LOG) }
)

// RFC 9659: the zstd content coding limits the window to 8MB, bounding decoder memory per request
private const val MAX_HTTP_WINDOW_LOG = 23
