package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.core.with
import org.http4k.multipart.Multipart
import org.http4k.multipart.MultipartForm
import java.util.*

object MultipartFormField : BiDiLensSpec<MultipartForm, String, String>("form",
    ParamMeta.StringParam,
    LensGet { name, form -> form.fields(name) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(Multipart.FormField(name, next)) }) }
)

object MultipartFormFile : BiDiLensSpec<MultipartForm, Multipart.FormFile, Multipart.FormFile>("form",
    ParamMeta.FileParam,
    LensGet { name, form -> form.files(name) },
    LensSet { _, values, target -> values.fold(target, { m, next -> m.plus(next) }) }
)

private data class MultipartBody(val boundary: String, val delegate: Body) : Body by delegate

fun Body.Companion.multipartForm(validator: FormValidator, vararg formFields: Lens<MultipartForm, *>): BiDiBodyLensSpec<MultipartForm> =
    BiDiBodyLensSpec(formFields.map { it.meta }, ContentType.MultipartForm(""),
        LensGet { _, target ->
            val actual = Header.Common.CONTENT_TYPE(target)
            val boundary = actual?.directive?.second ?: ""
            ContentNegotiation.Strict(ContentType.MultipartForm(boundary), actual)
            listOf(MultipartBody(boundary, target.body))
        },
        LensSet { _: String, values: List<Body>, target: HttpMessage ->
            values.fold(target) { a, b -> a.body(b) }
                .with(Header.Common.CONTENT_TYPE of ContentType.MultipartForm(UUID.randomUUID().toString()))
        }
    ).map({ (it as MultipartBody).let { MultipartForm.fromBody(it, it.boundary) } }, { it.toBody() })
