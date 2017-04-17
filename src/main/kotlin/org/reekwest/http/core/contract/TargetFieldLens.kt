package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

interface TargetFieldLens<IN, OUT> {
    /**
     * Lens operation to get the named value from the target
     */
    operator fun invoke(name: String, target: IN): List<OUT?>?

    /**
     * Lens operation to set the named value into the target
     */
    operator fun invoke(name: String, values: List<OUT>, target: IN): IN

    fun <NEXT> map(inFn: (OUT) -> NEXT?, outFn: (NEXT) -> OUT): TargetFieldLens<IN, NEXT> =
        object : TargetFieldLens<IN, NEXT> {
            override fun invoke(name: String, target: IN) = this@TargetFieldLens(name, target)?.map { it?.let(inFn) }
            override fun invoke(name: String, values: List<NEXT>, target: IN) = this@TargetFieldLens(name, values.map(outFn), target)
        }
}

fun <IN> TargetFieldLens<IN, String>.asByteBuffers() = map(String::asByteBuffer, ByteBuffer::asString)
