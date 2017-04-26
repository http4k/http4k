package org.reekwest.kontrakt

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer
import java.util.Collections.emptyList

typealias BodyLens<T> = Lens<HttpMessage, T>
typealias BiDiBodyLens<T> = BiDiLens<HttpMessage, T>

open class BodySpec<MID, out OUT>(private val delegate: LensSpec<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): BodyLens<OUT> = delegate.required("body", description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): BodySpec<MID, NEXT> = BodySpec(delegate.map(nextIn))
}

open class BiDiBodySpec<MID, OUT>(private val delegate: BiDiLensSpec<HttpMessage, MID, OUT>) : BodySpec<MID, OUT>(delegate) {
    override fun required(description: String?): BiDiBodyLens<OUT> = delegate.required("body", description)

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiBodySpec<MID, NEXT> = BiDiBodySpec(delegate.map(nextIn, nextOut))
}

object Body : BiDiBodySpec<ByteBuffer, ByteBuffer>(BiDiLensSpec("body",
    Get { _, target -> target.body?.let { listOf(it) } ?: emptyList() },
    Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }
)) {
    val string = map(ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null): BiDiBodyLens<String> = string.required(description)
}

