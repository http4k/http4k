package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.copy
import java.nio.ByteBuffer

object Body : BiDiLensSpec<HttpMessage, ByteBuffer, ByteBuffer>("body",
    GetLens({ _, target -> target.body?.let { listOf(it) } ?: emptyList() }, { it }),
    SetLens({ _, values, target -> values.fold(target) { a, b -> a.copy(body = b) } }, { it })
) {
    val string = map(ByteBuffer::asString, String::asByteBuffer)
    fun string(description: String? = null) = string.required("body", description)
}