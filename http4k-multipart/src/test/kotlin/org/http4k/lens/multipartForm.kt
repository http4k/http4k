package org.http4k.lens

import org.http4k.multipart.Multipart
import org.http4k.multipart.MultipartForm

object MultipartFormField : BiDiLensSpec<MultipartForm, String, String>("form",
    ParamMeta.StringParam,
    LensGet { name, form -> form.fields(name) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(Multipart.FormField(name, next)) }) }
)

object MultipartFormFile : BiDiLensSpec<MultipartForm, Multipart.FormFile, Multipart.FormFile>("form",
    ParamMeta.FileParam,
    LensGet { name, form -> form.files(name) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(next) }) }
)
//
//
//fun Body.Companion.multipartForm(validator: FormValidator, boundary: String, vararg formFields: Lens<MultipartForm, *>): BodyLensSpec<MultipartForm> =
//    BodyLensSpec<MultipartForm>(formFields.map { it.meta }, ContentType.MultipartForm(boundary),
//        LensGet { _, target ->
//            ContentNegotiation.StrictNoDirective(ContentType.MultipartForm(boundary), Header.Common.CONTENT_TYPE(target))
//            val a: MultipartForm = target.body.let { MultipartForm.fromBody(it, boundary) }
//            a
//        }
////        },
////        LensSet { _, values, target -> values.fold(target) { a, b -> a.body(b.b) }.with(Header.Common.CONTENT_TYPE of ContentType.MultipartForm(boundary)) }
//    )
//
//
//root(formFields.map { it.meta }, ContentType.MultipartForm(boundary), ContentNegotiation.StrictNoDirective)
//.map(ByteBuffer::asString, String::asByteBuffer)
//.map(
//{ Muli(formParametersFrom(it), emptyList()) },
//{ (fields) -> fields.flatMap { pair -> pair.value.map { pair.key to it } }.toUrlEncoded() })
//.map({ validateFields(it, validator, *formFields) },
//{ validateFields(it, validator, *formFields) })
