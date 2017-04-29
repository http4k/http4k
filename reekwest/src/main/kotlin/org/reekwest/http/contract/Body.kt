package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.contract.lens.Get
import org.reekwest.http.contract.lens.Set
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer
import java.util.Collections.emptyList

typealias BodyLens<T> = org.reekwest.http.contract.lens.Lens<HttpMessage, T>
typealias BiDiBodyLens<T> = org.reekwest.http.contract.lens.BiDiLens<HttpMessage, T>

open class BodySpec<MID, out OUT>(private val delegate: org.reekwest.http.contract.lens.LensSpec<HttpMessage, MID, OUT>) {
    open fun required(description: String? = null): org.reekwest.http.contract.BodyLens<OUT> = delegate.required("body", description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): org.reekwest.http.contract.BodySpec<MID, NEXT> = org.reekwest.http.contract.BodySpec(delegate.map(nextIn))
}

open class BiDiBodySpec<MID, OUT>(private val delegate: org.reekwest.http.contract.lens.BiDiLensSpec<HttpMessage, MID, OUT>) : org.reekwest.http.contract.BodySpec<MID, OUT>(delegate) {
    override fun required(description: String?): org.reekwest.http.contract.BiDiBodyLens<OUT> = delegate.required("body", description)

    fun <NEXT> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): org.reekwest.http.contract.BiDiBodySpec<MID, NEXT> = org.reekwest.http.contract.BiDiBodySpec(delegate.map(nextIn, nextOut))
}

object Body : org.reekwest.http.contract.BiDiBodySpec<ByteBuffer, ByteBuffer>(org.reekwest.http.contract.lens.BiDiLensSpec("body",
    Get { _, target -> target.body?.let { listOf(it) } ?: emptyList() },
    Set { _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }
)) {
    val string = org.reekwest.http.contract.Body.map(java.nio.ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null): org.reekwest.http.contract.BiDiBodyLens<String> = org.reekwest.http.contract.Body.string.required(description)
}

