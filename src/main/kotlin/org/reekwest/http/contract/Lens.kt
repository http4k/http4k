package org.reekwest.http.contract

typealias GetLens<IN, OUT> = (IN) -> List<OUT>

typealias SetLens<IN, OUT> = (List<OUT>, IN) -> IN

data class BiDiLens<IN, OUT>(val getLens: GetLens<IN,OUT>, val setLens: SetLens<IN,OUT>)

class MappableGetLens<in IN, MID, out OUT>(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String) = { target: IN -> rootFn(name, target).map(fn) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT): MappableGetLens<IN, MID, NEXT> = MappableGetLens(rootFn, { nextFn(fn(it)) })
}


class MappableSetLens<IN, MID, in OUT>(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String): SetLens<IN, OUT> = object : SetLens<IN, OUT> {
        override fun invoke(values: List<OUT>, target: IN): IN = rootFn(name, values.map(fn), target)
    }

    fun <NEXT> map(nextFn: (NEXT) -> OUT): MappableSetLens<IN, MID, NEXT> = MappableSetLens(rootFn, { fn(nextFn(it)) })
}