package org.http4k.multipart

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.multipart.internal.MultipartFormBuilder
import org.http4k.multipart.internal.StreamingMultipartFormParts
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*

sealed class Multipart {
    abstract val name: String
    internal abstract fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder


    data class FormField(override val name: String, val value: String) : Multipart() {
        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.field(name, value)
    }

    data class FormFile(override val name: String, val filename: String, val contentType: ContentType, val content: InputStream) : Multipart() {
        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.file(name, filename, contentType.value, content)
    }
}

data class MultipartForm(val formParts: List<Multipart>, val boundary: String = UUID.randomUUID().toString()) {
    constructor(vararg formParts: Multipart, boundary: String = UUID.randomUUID().toString()) : this(formParts.toList(), boundary)

    fun file(name: String): Multipart.FormFile? = files(name).firstOrNull()
    fun files(name: String): List<Multipart.FormFile> = formParts.filter { it.name == name }.mapNotNull { it as? Multipart.FormFile }

    fun field(name: String): String? = fields(name).firstOrNull()
    fun fields(name: String): List<String> = formParts.filter { it.name == name }.mapNotNull { it as? Multipart.FormField }.map { it.value }

    fun toBody(): Body =
        Body(ByteBuffer.wrap(
            formParts.fold(MultipartFormBuilder(boundary.toByteArray())) { memo, next ->
                next.applyTo(memo)
            }.build()))

    companion object {
        fun toMultipartForm(body: Body, boundary: String): MultipartForm {
            val form = StreamingMultipartFormParts.parse(boundary.toByteArray(StandardCharsets.UTF_8), body.stream, StandardCharsets.UTF_8)
            return MultipartForm(form.map {
                if (it.isFormField) Multipart.FormField(it.fieldName!!, it.contentsAsString)
                else Multipart.FormFile(it.fieldName!!, it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.inputStream)
            }, boundary)
        }
    }
}