package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

interface GetLens<in IN, out OUT> : (IN) -> List<OUT>

interface BiDiLens<IN, OUT> : GetLens<IN, OUT> {
    operator fun invoke(values: List<OUT>, target: IN): IN
}

fun <IN> Function1<String, BiDiLens<IN, String>>.asByteBuffers(): (String) -> BiDiLens<IN, ByteBuffer> = {
    object : BiDiLens<IN, ByteBuffer> {
        override fun invoke(values: List<ByteBuffer>, target: IN): IN =
            this@asByteBuffers(it)(values.map(ByteBuffer::asString), target)

        override fun invoke(target: IN): List<ByteBuffer> =
            this@asByteBuffers(it)(target).map { it.let(String::asByteBuffer) }
    }
}