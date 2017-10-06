package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.multipart.Multipart
import org.http4k.multipart.MultipartFormEntity
import java.io.InputStream
import java.util.*

data class FormFile(val filename: String, val contentType: ContentType, val content: InputStream)

object MultipartFormField : BiDiLensSpec<MultipartFormEntity, String, String>("form",
    ParamMeta.StringParam,
    LensGet { name, form -> form.fields(name) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(Multipart.FormField(name, next)) }) }
)

object MultipartFormFile : BiDiLensSpec<MultipartFormEntity, FormFile, FormFile>("form",
    ParamMeta.FileParam,
    LensGet { name, form -> form.files(name).map { FormFile(it.filename, it.contentType, it.content) } },
    LensSet { name, values, target ->
        values.fold(target, { m, (filename, contentType, content) ->
            m.plus(
                Multipart.FormFile(name, filename, contentType, content)
            )
        })
    }
)

private data class MultipartBody(val boundary: String, val delegate: Body) : Body by delegate

fun Body.Companion.multipartForm(validator: FormValidator, vararg formFields: Lens<MultipartFormEntity, *>): BiDiBodyLensSpec<MultipartFormEntity> =
    BiDiBodyLensSpec(formFields.map { it.meta }, ContentType.MULTIPART_FORM_DATA,
        LensGet { _, target ->
            val actual = Header.Common.CONTENT_TYPE(target)
            val boundary = actual?.directive?.second ?: ""
            ContentNegotiation.Strict(ContentType.MultipartFormWithBoundary(boundary), actual)
            listOf(MultipartBody(boundary, target.body))
        },
        LensSet { _: String, values: List<Body>, target: HttpMessage ->
            values.fold(target) { a, b -> a.body(b) }
                .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(UUID.randomUUID().toString()))
        }
    ).map({ (it as MultipartBody).let { MultipartFormEntity.fromBody(it, it.boundary) } }, MultipartFormEntity::toBody)

fun main(args: Array<String>) {
    MultipartFormField.boolean().required("boo")
    MultipartFormFile.required("boo")
}