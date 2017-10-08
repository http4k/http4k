package org.http4k.core

import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.multipart.MultipartFormBuilder
import org.http4k.multipart.MultipartFormMap.formParts
import org.http4k.multipart.Part
import org.http4k.multipart.StreamingMultipartFormParts
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.*

sealed class MultipartEntity {
    abstract val name: String
    internal abstract fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder

    data class Field(override val name: String, val value: String) : MultipartEntity() {
        override fun applyTo(builder: MultipartFormBuilder) = builder.field(name, value)
    }

    data class File(override val name: String, val file: FormFile) : MultipartEntity() {
        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.file(name, file.filename, file.contentType.value, file.content)
    }
}

data class MultipartFormBody(val formParts: List<MultipartEntity>, val boundary: String = UUID.randomUUID().toString()) : Body {

    constructor(vararg formParts: MultipartEntity, boundary: String = UUID.randomUUID().toString()) : this(formParts.toList(), boundary)

    fun file(name: String): MultipartEntity.File? = files(name).firstOrNull()
    fun files(name: String): List<MultipartEntity.File> = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.File }

    fun field(name: String): String? = fields(name).firstOrNull()
    fun fields(name: String): List<String> = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.Field }.map { it.value }

    companion object {
        private val DEFAULT_DISK_THRESHOLD = 10000

        fun from(httpMessage: HttpMessage, diskThreshold: Int = DEFAULT_DISK_THRESHOLD): MultipartFormBody {
            val boundary = CONTENT_TYPE(httpMessage)?.directive?.second ?: ""
            val form = StreamingMultipartFormParts.parse(boundary.toByteArray(UTF_8), httpMessage.body.stream, UTF_8)
            val dir = Files.createTempDirectory("http4k-mp").toFile().apply { deleteOnExit() }

            val parts = formParts(form, UTF_8, diskThreshold, dir).map {
                if (it.isFormField) MultipartEntity.Field(it.fieldName!!, it.string())
                else MultipartEntity.File(it.fieldName!!, FormFile(it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.newInputStream))
            }
            return MultipartFormBody(parts, boundary)
        }
    }

    @JvmName("plusField")
    fun plus(field: Pair<String, String>): MultipartFormBody = copy(formParts = formParts + MultipartEntity.Field(field.first, field.second))

    @JvmName("plusFile")
    fun plus(field: Pair<String, FormFile>): MultipartFormBody = copy(formParts = formParts + MultipartEntity.File(field.first, field.second))

    override val stream: InputStream by lazy { formParts.fold(MultipartFormBuilder(boundary.toByteArray())) { memo, next -> next.applyTo(memo) }.stream() }
    override val payload: ByteBuffer by lazy { stream.use { ByteBuffer.wrap(it.readBytes()) } }
    override fun toString(): String = String(payload.array())
}

internal fun Part.string(): String = when (this) {
    is Part.DiskBacked -> throw RuntimeException("wat?")
    is Part.InMemory -> String(bytes, encoding)
}