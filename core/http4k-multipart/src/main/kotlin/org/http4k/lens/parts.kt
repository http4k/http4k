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

@ExposedCopyVisibility
data class MultipartFormField internal constructor(
    val value: String,
    val headers: Headers = emptyList(),
    val closeable: Closeable
): Closeable by closeable {

    constructor(value: String, headers: Headers = emptyList()): this(value, headers, {})

    companion object : BiDiLensSpec<MultipartForm, MultipartFormField>("form",
        StringParam,
        LensGet { name, (fields) -> fields.getOrDefault(name, listOf()) },
        LensSet { name, values, target -> values.fold(target.minusField(name)) { m, next -> m + (name to next) } }
    ) {
        fun string() = map({ it.value }, { MultipartFormField(it) })
    }
}

@ExposedCopyVisibility
data class MultipartFormFile internal constructor(
    val filename: String,
    val contentType: ContentType,
    val content: InputStream,
    val closeable: Closeable
) : Closeable {

    constructor(filename: String, contentType: ContentType, content: InputStream) : this(
        filename,
        contentType,
        content,
        Closeable { })

    override fun close() {
        content.close()
        closeable.close()
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
