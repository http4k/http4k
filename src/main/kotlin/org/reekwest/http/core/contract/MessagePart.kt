package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.Response

abstract class MessagePart<in IN, out RAW, out OUT>(val location: String, val fn: (IN) -> RAW?) {
    abstract operator fun get(m: IN): OUT

    internal fun <NEXT> mapValid(next: (RAW) -> NEXT): (IN) -> NEXT? = {
        try {
            fn(it)?.let(next)
        } catch (e: Exception) {
            throw Invalid(this)
        }
    }

}

operator fun <T, FINAL> Request.get(param: MessagePart<Request, T, FINAL>): FINAL = param[this]
operator fun <T, FINAL> Response.get(param: MessagePart<Response, T, FINAL>): FINAL = param[this]

open class Optional<in IN, out OUT>(location: String, fn: (IN) -> OUT?) : MessagePart<IN, OUT, OUT?>(location, fn) {
    override operator fun get(m: IN): OUT? = fn(m)
    fun <X> map(next: (OUT) -> X) = Optional(location, mapValid(next))
}

open class Required<in IN, out OUT>(location: String, fn: (IN) -> OUT?) : MessagePart<IN, OUT, OUT>(location, fn) {
    override operator fun get(m: IN): OUT = fn(m) ?: throw Missing(this)
    fun <X> map(next: (OUT) -> X) = Required(location, mapValid(next))
}

