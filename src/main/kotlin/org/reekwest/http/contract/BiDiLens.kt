package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

@FunctionalInterface
interface GetLens<in IN, out OUT> {
    operator fun invoke(target: IN): List<OUT>
}

@FunctionalInterface
interface SetLens<IN, in OUT> {
    operator fun invoke(values: List<OUT>, target: IN): IN
}

interface BiDiLens<IN, OUT> : GetLens<IN, OUT>, SetLens<IN, OUT>

@JvmName("get")
fun <IN> Function1<String, GetLens<IN, String>>.asByteBuffers(): (String) -> GetLens<IN, ByteBuffer> = {
    object : GetLens<IN, ByteBuffer> {
        override fun invoke(target: IN): List<ByteBuffer> =
            this@asByteBuffers(it)(target).map { it.let(String::asByteBuffer) }
    }
}

@JvmName("set")
fun <IN> Function1<String, SetLens<IN, String>>.asByteBuffers(): (String) -> SetLens<IN, ByteBuffer> = {
    object : SetLens<IN, ByteBuffer> {
        override fun invoke(values: List<ByteBuffer>, target: IN): IN =
            this@asByteBuffers(it)(values.map(ByteBuffer::asString), target)
    }
}

fun <IN> Function1<String, BiDiLens<IN, String>>.asByteBuffers(): (String) -> BiDiLens<IN, ByteBuffer> = {
    object : BiDiLens<IN, ByteBuffer> {
        override fun invoke(values: List<ByteBuffer>, target: IN): IN =
            this@asByteBuffers(it)(values.map(ByteBuffer::asString), target)

        override fun invoke(target: IN): List<ByteBuffer> =
            this@asByteBuffers(it)(target).map { it.let(String::asByteBuffer) }
    }
}