package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

interface NamedLens<IN, OUT> {
    operator fun invoke(name: String, target: IN): List<OUT?>?
    operator fun invoke(name: String, values: List<OUT>, target: IN): IN

    fun <NEXT> map(inFn: (OUT) -> NEXT?, outFn: (NEXT) -> OUT): NamedLens<IN, NEXT> {
        val sup = this
        return object : NamedLens<IN, NEXT> {
            override fun invoke(name: String, target: IN) = sup(name, target)?.map { it?.let(inFn) }
            override fun invoke(name: String, values: List<NEXT>, target: IN) = sup(name, values.map(outFn), target)
        }
    }
}

fun <IN> NamedLens<IN, String>.asByteBuffers() = map(String::asByteBuffer, ByteBuffer::asString)
