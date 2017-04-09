package org.reekwest.http.core.contract

import org.reekwest.http.core.*

object Query {
    fun optional(name: String, description: String? = null) = Optional<Request, String>(Meta(name, "query", description)) { it.query(name) }
    fun required(name: String, description: String? = null) = Required<Request, String>(Meta(name, "query", description)) { it.query(name) }

    object multi {
        fun optional(name: String, description: String? = null) = Optional<Request, List<String?>>(Meta(name, "query", description)) { it.queries(name) }
        fun required(name: String, description: String? = null) = Required<Request, List<String?>>(Meta(name, "query", description)) {
            val values = it.queries(name)
            if (values.isEmpty()) null else values
        }
    }
}


sealed class ContractBreach2(meta: Meta) : Exception(meta.toString())

class Missing2(meta: Meta) : ContractBreach2(meta)
class Invalid2(meta: Meta) : ContractBreach2(meta)


abstract class MsgPart<in IN : HttpMessage, in OUT, out FINAL>(val meta: Meta, private val spec: Spec<IN, OUT>) {
    operator fun get(m: IN): FINAL = convert(spec.fn(m, meta.name))
    abstract internal fun convert(o: OUT?): FINAL
}

open class Spec<in IN : HttpMessage, OUT>(private val location: String, val fn: (IN, String) -> OUT?) {
    fun <NEXT> map(next: (OUT) -> NEXT): Spec<IN, NEXT> = Spec(location) { req, name -> fn(req, name)?.let(next) }

    fun optional(name: String, description: String? = null): MsgPart<IN, OUT, OUT?> = object : MsgPart<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convert(o: OUT?): OUT? = o
    }

    fun required(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convert(o: OUT?): OUT = o ?: throw Missing2(meta)
    }
}

object H2 : Spec<Request, String>("header", { req, name -> req.header(name) })
