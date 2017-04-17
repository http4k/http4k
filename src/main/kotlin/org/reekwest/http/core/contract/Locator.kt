package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer

interface Locator<IN, OUT> {
    val location: String
    fun get(target: IN, name: String): List<OUT?>?
    fun set(target: IN, name: String, values: List<OUT>): IN

    fun <NEXT> map(inFn: (OUT) -> NEXT?, outFn: (NEXT) -> OUT): Locator<IN, NEXT> {
        val sup = this
        return object : Locator<IN, NEXT> {
            override val location = sup.location
            override fun get(target: IN, name: String) = sup.get(target, name)?.map { it?.let(inFn) }
            override fun set(target: IN, name: String, values: List<NEXT>) = sup.set(target, name, values.map(outFn))
        }
    }
}


internal class StringLocator<IN>(override val location: String,
                                 val getFn: (IN, String) -> List<String?>?,
                                 val setFn: (IN, String, List<String>) -> IN) : Locator<IN, ByteBuffer> {
    override fun get(target: IN, name: String) = getFn(target, name)?.mapNotNull { it -> it?.asByteBuffer() }
    override fun set(target: IN, name: String, values: List<ByteBuffer>) = setFn(target, name, values.map(ByteBuffer::asString))
}
