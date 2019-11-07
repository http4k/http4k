package org.http4k.lens

import org.http4k.core.ContentType
import org.http4k.core.Headers
import java.io.Closeable
import java.io.InputStream

data class MultipartFormField(val value: String, val headers: Headers = emptyList()) {
    companion object : BiDiLensSpec<MultipartForm, MultipartFormField>("form",
        ParamMeta.StringParam,
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
        ParamMeta.FileParam,
        LensGet { name, form ->
            form.files[name]?.map { MultipartFormFile(it.filename, it.contentType, it.content) }
                ?: emptyList()
        },
        LensSet { name, values, target -> values.fold(target.minusFile(name)) { m, next -> m + (name to next) } }
    )
}