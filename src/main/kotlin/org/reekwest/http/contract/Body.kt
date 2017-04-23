package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer

open class BodySpec<in IN : HttpMessage, MID, out OUT>(private val delegate: LensSpec<IN, MID, OUT>) {
    open fun required(description: String? = null) = delegate.required("body", description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodySpec<IN, MID, NEXT> = BodySpec(delegate.map(nextIn))
}

open class BiDiBodySpec<in IN : HttpMessage, MID, OUT>(private val delegate: BiDiLensSpec<IN, MID, OUT>) : BodySpec<IN, MID, OUT>(delegate) {
    override fun required(description: String?) = delegate.required("body", description)

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiBodySpec<IN, MID, NEXT> = BiDiBodySpec(delegate.map(nextIn, nextOut))
}

object Body : BiDiBodySpec<HttpMessage, ByteBuffer, ByteBuffer>(BiDiLensSpec("body",
    Get { _, target -> target.body?.let { listOf(it) } ?: emptyList() },
    Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }
)) {
    val string = map(ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null) = string.required(description)
}