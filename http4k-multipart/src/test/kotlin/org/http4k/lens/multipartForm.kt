package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.multipart.MultipartFormEntity
import java.io.InputStream
import java.util.*

data class FormFile(val filename: String, val contentType: ContentType, val content: InputStream) {
    companion object : BiDiLensSpec<MultipartForm, FormFile, FormFile>("form",
        ParamMeta.FileParam,
        LensGet { name, form -> form.files[name]?.map { FormFile(it.filename, it.contentType, it.content) } ?: emptyList() },
        LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
    )
}

data class MultipartForm(val fields: Map<String, List<String>> = emptyMap(),
                         val files: Map<String, List<FormFile>> = emptyMap(),
                         val errors: List<Failure> = emptyList()) {

    @JvmName("plusField")
    operator fun plus(kv: Pair<String, String>): MultipartForm =
        copy(fields = fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    @JvmName("plusFile")
    operator fun plus(kv: Pair<String, FormFile>): MultipartForm =
        copy(files = files.plus(kv.first to files.getOrDefault(kv.first, emptyList()).plus(kv.second)))
}

fun Body.Companion.multipartForm(validator: FormValidator, vararg parts: Lens<MultipartForm, *>): BiDiBodyLensSpec<MultipartForm> =
    BiDiBodyLensSpec(parts.map { it.meta }, ContentType.MULTIPART_FORM_DATA,
        LensGet { _, target ->
            val actual = Header.Common.CONTENT_TYPE(target)
            val boundary = actual?.directive?.second ?: ""
            ContentNegotiation.Strict(ContentType.MultipartFormWithBoundary(boundary), actual)
            listOf(MultipartBody(boundary, target.body))
        },
        LensSet { _: String, values: List<Body>, target: HttpMessage ->
            values.fold(target) { a, b -> a.body(b) }
                .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(UUID.randomUUID().toString()))
        })
        .map(Body::asMultipartFormEntity, MultipartFormEntity::toBody)
        .map(MultipartFormEntity::toMultipartForm, MultipartForm::toMultipartFormEntity)

fun MultipartFormEntity.toMultipartForm() = MultipartForm()
fun MultipartForm.toMultipartFormEntity() = MultipartFormEntity()

private fun Body.asMultipartFormEntity() = (this as MultipartBody).let { MultipartFormEntity.fromBody(it, it.boundary) }

private data class MultipartBody(val boundary: String, val delegate: Body) : Body by delegate
