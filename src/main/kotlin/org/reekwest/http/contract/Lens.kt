package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

interface Lens<IN, OUT> {
    operator fun invoke(target: IN): List<OUT?>?
    operator fun invoke(values: List<OUT>, target: IN): IN
}

fun <IN> Function1<String, Lens<IN, String>>.asByteBuffers(): (String) -> Lens<IN, ByteBuffer> {
    // FIXME remove this
    val bob = this
    return {
        object : Lens<IN, ByteBuffer> {
            override fun invoke(values: List<ByteBuffer>, target: IN): IN =
                bob(it)(values.map(ByteBuffer::asString), target)

            override fun invoke(target: IN): List<ByteBuffer?>? =
                bob(it)(target)?.map { it?.let(String::asByteBuffer) }
        }
    }
}