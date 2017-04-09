package org.reekwest.http.core.contract

interface MultiSpec<in IN, OUT> {
    fun optional(name: String, description: String? = null): MsgPart<IN, OUT, List<OUT?>?>
    fun required(name: String, description: String? = null): MsgPart<IN, OUT, List<OUT?>>
}

open class Spec<in IN, OUT>(private val location: String, val fn: (IN, String) -> List<OUT?>?) {
    fun <NEXT> map(next: (OUT) -> NEXT): Spec<IN, NEXT> = Spec(location)
    { req, name -> fn(req, name)?.let { it.map { it?.let(next) } } }

    fun optional(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT?>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT? = o?.firstOrNull()
    }

    fun required(name: String, description: String? = null) = object : MsgPart<IN, OUT, OUT>(Meta(name, location, description), this) {
        override fun convert(o: List<OUT?>?): OUT = o?.firstOrNull() ?: throw Missing(meta)
    }

    internal val id: Spec<IN, OUT>
        get() = this

    val multi = object : MultiSpec<IN, OUT> {
        override fun optional(name: String, description: String?): MsgPart<IN, OUT, List<OUT?>?> = object : MsgPart<IN, OUT, List<OUT?>?>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?) = o
        }

        override fun required(name: String, description: String?) = object : MsgPart<IN, OUT, List<OUT?>>(Meta(name, location, description), id) {
            override fun convert(o: List<OUT?>?): List<OUT?> {
                val orEmpty = o ?: emptyList()
                return if (orEmpty.isEmpty()) throw Missing(meta) else orEmpty
            }
        }
    }
}
