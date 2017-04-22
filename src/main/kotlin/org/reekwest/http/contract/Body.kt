package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer

open class BodySpec<OUT>(private val delegate: LensSpec<HttpMessage, ByteBuffer, OUT>) {
    open fun required(description: String? = null) = delegate.required(delegate.location, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodySpec<NEXT> = BodySpec(delegate.map(nextIn))
}

open class BiDiBodySpec<OUT>(private val delegate: BiDiLensSpec<HttpMessage, ByteBuffer, OUT>) : BodySpec<OUT>(delegate) {
    override fun required(description: String?) = delegate.required(delegate.location, description)

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiBodySpec<NEXT> = BiDiBodySpec(delegate.map(nextIn, nextOut))
}

object Body : BiDiBodySpec<ByteBuffer>(BiDiLensSpec<HttpMessage, ByteBuffer, ByteBuffer>("body",
    Get { _, target -> target.body?.let { listOf(it) } ?: emptyList() },
    Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }
)) {
    val string = map(ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null) = string.required(description)
}