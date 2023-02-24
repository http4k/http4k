package org.http4k.lens

import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Headers
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.StringParam
import java.io.Closeable
import java.io.InputStream
import kotlin.Int.Companion.MAX_VALUE
import kotlin.random.Random.Default.nextInt

data class MultipartFormField(val value: String, val headers: Headers = emptyList()) {
    companion object : BiDiLensSpec<MultipartForm, MultipartFormField>("form",
        StringParam,
        LensGet { name, (fields) -> fields.getOrDefault(name, listOf()) },
        LensSet { name, values, target -> values.fold(target.minusField(name)) { m, next -> m + (name to next) } }
    ) {
        fun string() = map({ it.value }, { MultipartFormField(it) })
    }
}

data class MultipartFormFile(val filename: String, val contentType: ContentType, val content: InputStream) : Closeable {
    override fun close() {
        content.close()
    }

    private data class Realised(val filename: String, val contentType: ContentType, val content: String)

    private val realised by lazy { Realised(filename, contentType, content.use { String(it.readBytes()) }) }

    override fun toString(): String = realised.toString()

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is MultipartFormFile? -> false
        else -> realised == other?.realised
    }

    override fun hashCode(): Int = realised.hashCode()

    companion object : BiDiLensSpec<MultipartForm, MultipartFormFile>("form",
        FileParam,
        LensGet { name, form ->
            form.files[name]?.map { MultipartFormFile(it.filename, it.contentType, it.content) }
                ?: emptyList()
        },
        LensSet { name, values, target -> values.fold(target.minusFile(name)) { m, next -> m + (name to next) } }
    ) {
        /**
         * Use this when it doesn't matter about the name or content type of the file uploaded.
         */
        fun inputStream() = map({ it.content }, { MultipartFormFile(nextInt(0, MAX_VALUE).toString(), OCTET_STREAM, it) })
    }
}
