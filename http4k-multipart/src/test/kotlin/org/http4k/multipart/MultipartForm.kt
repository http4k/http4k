package org.http4k.multipart

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.multipart.internal.MultipartFormBuilder
import org.http4k.multipart.internal.MultipartFormMap
import org.http4k.multipart.internal.StreamingMultipartFormParts
import org.http4k.multipart.internal.string
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

sealed class Multipart {
    abstract val name: String
    internal abstract fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder

    data class FormField(override val name: String, val value: String) : Multipart() {
        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.field(name, value)
    }

    data class FormFile(override val name: String, val filename: String, val contentType: ContentType, val content: InputStream) : Multipart() {
        private data class RealisedFormField(val name: String, val filename: String, val contentType: ContentType, val content: String)

        private val realised by lazy { RealisedFormField(name, filename, contentType, content.use { String(it.readBytes()) }) }

        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.file(name, filename, contentType.value, content)

        override fun toString(): String = realised.toString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is FormFile?) return false
            return realised == other?.realised
        }

        override fun hashCode(): Int = realised.hashCode()
    }
}

data class MultipartForm(val formParts: List<Multipart>, val boundary: String = UUID.randomUUID().toString()) {
    constructor(vararg formParts: Multipart, boundary: String = UUID.randomUUID().toString()) : this(formParts.toList(), boundary)

    fun file(name: String): Multipart.FormFile? = files(name).firstOrNull()
    fun files(name: String): List<Multipart.FormFile> = formParts.filter { it.name == name }.mapNotNull { it as? Multipart.FormFile }

    fun field(name: String): String? = fields(name).firstOrNull()
    fun fields(name: String): List<String> = formParts.filter { it.name == name }.mapNotNull { it as? Multipart.FormField }.map { it.value }

    fun toBody(): Body =
        Body(formParts.fold(MultipartFormBuilder(boundary.toByteArray())) { memo, next -> next.applyTo(memo) }.stream())

    companion object {
        fun fromBody(body: Body, boundary: String): MultipartForm {
            val form = StreamingMultipartFormParts.parse(boundary.toByteArray(StandardCharsets.UTF_8), body.stream, StandardCharsets.UTF_8)
            val parts = MultipartFormMap.formParts(form, StandardCharsets.UTF_8, 10000, File("./out/tmp"))
            return MultipartForm(parts.map {
                if (it.isFormField) Multipart.FormField(it.fieldName!!, it.string())
                else Multipart.FormFile(it.fieldName!!, it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.newInputStream)
            }, boundary)
        }
    }
}