package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.FormFile
import org.http4k.core.HttpMessage
import org.http4k.core.MultipartEntity
import org.http4k.core.MultipartFormBody
import org.http4k.core.with
import java.io.Closeable
import java.util.*

object MultipartFormField : BiDiLensSpec<MultipartForm, String, String>("form",
    ParamMeta.StringParam,
    LensGet { name, (fields) -> fields.getOrDefault(name, listOf()) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)

object MultipartFormFile : BiDiLensSpec<MultipartForm, FormFile, FormFile>("form",
    ParamMeta.FileParam,
    LensGet { name, form -> form.files[name]?.map { FormFile(it.filename, it.contentType, it.content) } ?: emptyList() },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)

data class MultipartForm(val fields: Map<String, List<String>> = emptyMap(),
                         val files: Map<String, List<FormFile>> = emptyMap(),
                         val errors: List<Failure> = emptyList()) : Closeable {

    override fun close() {
        files.values.flatten().forEach(FormFile::close)
    }

    @JvmName("plusField")
    operator fun plus(kv: Pair<String, String>): MultipartForm =
        copy(fields = fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    @JvmName("plusFile")
    operator fun plus(kv: Pair<String, FormFile>): MultipartForm =
        copy(files = files.plus(kv.first to files.getOrDefault(kv.first, emptyList()).plus(kv.second)))
}

val MULTIPART_BOUNDARY = UUID.randomUUID().toString()

fun Body.Companion.multipartForm(validator: Validator, vararg parts: Lens<MultipartForm, *>, defaultBoundary: String = MULTIPART_BOUNDARY, diskThreshold: Int = MultipartFormBody.DEFAULT_DISK_THRESHOLD): BiDiBodyLensSpec<MultipartForm> =
    BiDiBodyLensSpec(parts.map { it.meta }, ContentType.MULTIPART_FORM_DATA,
        LensGet { _, target ->
            listOf(MultipartFormBody.from(target, diskThreshold).apply {
                ContentNegotiation.Strict(ContentType.MultipartFormWithBoundary(boundary), Header.Common.CONTENT_TYPE(target))
            })
        },
        LensSet { _: String, values: List<Body>, target: HttpMessage ->
            values.fold(target) { a, b ->
                a.body(b)
                    .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(defaultBoundary))
            }
        })
        .map({ it.toMultipartForm() }, { it.toMultipartFormBody(defaultBoundary) })
        .map({ it.copy(errors = validator(it, *parts)) }, { it.copy(errors = validator(it, *parts)) })

internal fun Body.toMultipartForm(): MultipartForm = (this as MultipartFormBody).let {
    it.formParts.fold(MultipartForm()) { memo, next ->
        when (next) {
            is MultipartEntity.File -> memo + (next.name to next.file)
            is MultipartEntity.Field -> memo + (next.name to next.value)
        }
    }
}

internal fun MultipartForm.toMultipartFormBody(boundary: String): MultipartFormBody {
    val withFields = fields.toList()
        .fold(MultipartFormBody(boundary = boundary)) { body, (name, values) ->
            values.fold(body) { bodyMemo, fieldValue ->
                bodyMemo.plus(name to fieldValue)
            }
        }

    return files.toList()
        .fold(withFields) { body, (name, values) ->
            values.fold(body) { bodyMemo, file ->
                bodyMemo.plus(name to file)
            }
        }
}
