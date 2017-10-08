package org.http4k.core

import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.MultipartForm
import org.http4k.lens.ParamMeta
import java.io.InputStream

data class FormFile(val filename: String, val contentType: ContentType, val content: InputStream) {

    private data class Realised(val filename: String, val contentType: ContentType, val content: String)

    private val realised by lazy { Realised(filename, contentType, content.use { String(it.readBytes()) }) }

    override fun toString(): String = realised.toString()

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other !is FormFile? -> false
        else -> realised == other?.realised
    }

    override fun hashCode(): Int = realised.hashCode()

    companion object : BiDiLensSpec<MultipartForm, FormFile, FormFile>("form",
        ParamMeta.FileParam,
        LensGet { name, form -> form.files[name]?.map { FormFile(it.filename, it.contentType, it.content) } ?: emptyList() },
        LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
    )
}