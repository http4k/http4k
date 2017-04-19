package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.contract.BiDiMapper.Companion.Identity
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer

open class BodySpec<OUT : Any>(private val delegate: LensSpec<HttpMessage, OUT>) {
    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT) = BodySpec(delegate.map(nextIn))

    fun <NEXT : Any> map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT) = BodySpec(delegate.map(nextIn, nextOut))

    fun required(description: String? = null) = delegate.required("body", description)
}

object Body : BodySpec<ByteBuffer>(LensSpec("body",
    {
        object : Lens<HttpMessage, ByteBuffer> {
            override fun invoke(target: HttpMessage): List<ByteBuffer?>? = listOf(target.body)
            override fun invoke(values: List<ByteBuffer?>?, target: HttpMessage): HttpMessage =
                values?.let { it.fold(target) { memo, next -> next?.let { memo.copy(body = it) } ?: memo } } ?: target
        }
    }, Identity())) {

    val string = Body.map(ByteBuffer::asString, String::asByteBuffer)

    fun binary(description: String? = null) = Body.required(description)
    fun string(description: String? = null) = Body.string.required(description)

}
