package org.http4k.core

import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.multipart.MultipartFormBuilder
import org.http4k.multipart.MultipartFormParser
import org.http4k.multipart.Part
import org.http4k.multipart.StreamingMultipartFormParts
import java.io.ByteArrayInputStream
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.UUID

sealed class MultipartEntity : Closeable {
    abstract val name: String
    internal abstract fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder

    data class Field(override val name: String, val value: String) : MultipartEntity() {
        override fun close() = Unit

        override fun applyTo(builder: MultipartFormBuilder) = builder.field(name, value)
    }

    data class File(override val name: String, val file: FormFile) : MultipartEntity() {
        override fun close() = file.content.close()

        override fun applyTo(builder: MultipartFormBuilder): MultipartFormBuilder = builder.file(name, file.filename, file.contentType.value, file.content)
    }
}

fun HttpMessage.multipartIterator(): Iterator<MultipartEntity> {
    val boundary = CONTENT_TYPE(this)?.directive?.second ?: ""

    return StreamingMultipartFormParts.parse(boundary.toByteArray(UTF_8), body.stream, UTF_8)
            .asSequence()
            .map {
                if (it.isFormField) MultipartEntity.Field(it.fieldName!!, it.contentsAsString)
                else MultipartEntity.File(it.fieldName!!, FormFile(it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.inputStream))
            }.iterator()
}

/**
 * Represents a Multi-part that is backed by a stream, which should be closed after handling the content. The gotchas
 * which apply to StreamBody also apply here..
 **/
data class MultipartFormBody private constructor(internal val formParts: List<MultipartEntity>, val boundary: String = UUID.randomUUID().toString()) : Body, Closeable {
    /**
     * @throws IllegalStateException if there is no supplied body length
     */
    override val length: Long by lazy { throw IllegalStateException("Length is not available in MultipartFormBody") }

    constructor(boundary: String = UUID.randomUUID().toString()) : this(emptyList(), boundary)

    override fun close() = formParts.forEach(MultipartEntity::close)

    fun file(name: String) = files(name).firstOrNull()
    fun files(name: String) = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.File }.map { it.file }

    fun field(name: String) = fields(name).firstOrNull()
    fun fields(name: String) = formParts.filter { it.name == name }.mapNotNull { it as? MultipartEntity.Field }.map { it.value }

    @JvmName("plusField")
    operator fun plus(field: Pair<String, String>) = copy(formParts = formParts + MultipartEntity.Field(field.first, field.second))

    @JvmName("plusFile")
    operator fun plus(field: Pair<String, FormFile>) = copy(formParts = formParts + MultipartEntity.File(field.first, field.second))

    override val stream by lazy { formParts.fold(MultipartFormBuilder(boundary.toByteArray())) { memo, next -> next.applyTo(memo) }.stream() }
    override val payload: ByteBuffer by lazy { stream.use { ByteBuffer.wrap(it.readBytes()) } }
    override fun toString() = String(payload.array())

    companion object {
        const val DEFAULT_DISK_THRESHOLD = 1000 * 1024

        fun from(httpMessage: HttpMessage, diskThreshold: Int = DEFAULT_DISK_THRESHOLD): MultipartFormBody {
            val boundary = CONTENT_TYPE(httpMessage)?.directive?.second ?: ""
            val inputStream = httpMessage.body.run { if (stream.available() > 0) stream else ByteArrayInputStream(payload.array()) }
            val form = StreamingMultipartFormParts.parse(boundary.toByteArray(UTF_8), inputStream, UTF_8)
            val dir = Files.createTempDirectory("http4k-mp").toFile().apply { deleteOnExit() }

            val parts = MultipartFormParser(UTF_8, diskThreshold, dir).formParts(form).map {
                if (it.isFormField) MultipartEntity.Field(it.fieldName!!, it.string(diskThreshold))
                else MultipartEntity.File(it.fieldName!!, FormFile(it.fileName!!, ContentType(it.contentType!!, ContentType.TEXT_HTML.directive), it.newInputStream))
            }
            return MultipartFormBody(parts, boundary)
        }
    }
}

internal fun Part.string(diskThreshold: Int = MultipartFormBody.DEFAULT_DISK_THRESHOLD): String = when (this) {
    is Part.DiskBacked -> throw RuntimeException("Fields configured to not be greater than $diskThreshold bytes.")
    is Part.InMemory -> String(bytes, encoding)
}