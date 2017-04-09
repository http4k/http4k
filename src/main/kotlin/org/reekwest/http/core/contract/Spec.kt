package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage

open class Spec<in IN : HttpMessage, OUT>(private val location: String, val fn: (IN, String) -> List<OUT?>?) {
    fun <NEXT> map(next: (OUT) -> NEXT): Spec<IN, NEXT> = Spec(location)
    { req, name -> fn(req, name)?.let { it.map { it?.let(next) } } }

    fun optional(name: String, description: String? = null): MsgPart<IN, OUT, OUT?> = object : MsgPart<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT? = o?.firstOrNull()
    }

    fun required(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT = o?.firstOrNull() ?: throw Missing(meta)
    }
}