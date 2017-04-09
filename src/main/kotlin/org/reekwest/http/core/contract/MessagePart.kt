package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.Response

open class MessagePart<in IN, out OUT>(val fn: (IN) -> OUT?) {
    internal fun <X> mapValid(next: (OUT) -> X): (IN) -> X? {
        return {
            try {
                fn(it)?.let(next)
            } catch (e: Exception) {
                throw Invalid(this)
            }
        }
    }
}

operator fun <T> Request.get(param: Optional<Request, T>): T? = param[this]
operator fun <T> Response.get(param: Optional<Response, T>): T? = param[this]

operator fun <T> Request.get(param: Required<Request, T>): T = param[this]
operator fun <T> Response.get(param: Required<Response, T>): T = param[this]

open class Optional<in IN, out OUT>(fn: (IN) -> OUT?) : MessagePart<IN, OUT>(fn) {
    operator fun get(m: IN): OUT? = fn(m)
    fun <X> map(next: (OUT) -> X) = Optional(mapValid(next))
}

open class Required<in IN, out OUT>(fn: (IN) -> OUT?) : MessagePart<IN, OUT>(fn) {
    operator fun get(m: IN): OUT = fn(m) ?: throw Missing(this)
    fun <X> map(next: (OUT) -> X) = Required(mapValid(next))
}

