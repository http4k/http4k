package org.http4k.multipart

import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.multipart.internal.MultipartFormBuilder
import org.http4k.multipart.internal.MultipartFormMap.formParts
import org.http4k.multipart.internal.StreamingMultipartFormParts
import org.http4k.multipart.internal.string
import java.io.InputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.*

sealed class MultipartEntity {
    abstract val name: String
    internal abstract fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder

    data class Form(override val name: String, val value: String) : MultipartEntity() {
        override fun applyTo(builder: MultipartFormBuilder) = builder.field(name, value)
    }

    data class File(override val name: String, val filename: String, val contentType: ContentType, val content: InputStream) : MultipartEntity() {
        private data class RealisedFormField(val name: String, val filename: String, val contentType: ContentType, val content: String)

        private val realised by lazy { RealisedFormField(name, filename, contentType, content.use { String(it.readBytes()) }) }

        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.file(name, filename, contentType.value, content)

        override fun toString(): String = realised.toString()

        override fun equals(other: Any?): Boolean = when {
            this === other -> true
            other !is File? -> false
            else -> realised == other?.realised
        }

        override fun hashCode(): Int = realised.hashCode()
    }
}

data class MultipartFormEntity(private val formParts: List<MultipartEntity>, val boundary: String = UUID.randomUUID().toString()) {
    constructor(vararg formParts: MultipartEntity, boundary: String = UUID.randomUUID().toString()) : this(formParts.toList(), boundary)

    fun file(name: String): MultipartEntity.File? = files(name).firstOrNull()
    fun files(name: String): List<MultipartEntity.File> = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.File }

    fun field(name: String): String? = fields(name).firstOrNull()
    fun fields(name: String): List<String> = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.Form }.map { it.value }

    fun toBody(): Body = Body(formParts.fold(MultipartFormBuilder(boundary.toByteArray())) { memo, next -> next.applyTo(memo) }.stream())

    companion object {
        private val DEFAULT_DISK_THRESHOLD = 10000

        fun fromBody(body: Body, boundary: String, diskThreshold: Int = DEFAULT_DISK_THRESHOLD): MultipartFormEntity {
            val form = StreamingMultipartFormParts.parse(boundary.toByteArray(UTF_8), body.stream, UTF_8)
            val dir = Files.createTempDirectory("http4k-mp").toFile().apply { this.deleteOnExit() }
            val parts = formParts(form, UTF_8, diskThreshold, dir).map {
                if (it.isFormField) MultipartEntity.Form(it.fieldName!!, it.string())
                else MultipartEntity.File(it.fieldName!!, it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.newInputStream)
            }
            return MultipartFormEntity(parts, boundary)
        }
    }

    operator fun plus(part: MultipartEntity): MultipartFormEntity = copy(formParts = formParts + part)
}