package org.reekwest.http.core.contract

import org.reekwest.http.asString
import org.reekwest.http.asByteBuffer
import java.nio.ByteBuffer

interface Locator<IN, OUT> {
    val name: String
    fun get(target: IN, name: String): List<OUT?>?
    fun set(target: IN, name: String, values: List<OUT>): IN
}

internal class StringLocator<IN>(override val name: String,
                        val getFn: (IN, String) -> List<String?>?,
                        val setFn: (IN, String, List<String>) -> IN) : Locator<IN, ByteBuffer> {
    override fun get(target: IN, name: String) = getFn(target, name)?.mapNotNull { it -> it?.asByteBuffer() }
    override fun set(target: IN, name: String, values: List<ByteBuffer>) = setFn(target, name, values.map(ByteBuffer::asString))
}
