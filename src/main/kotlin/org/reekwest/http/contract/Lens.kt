package org.reekwest.http.contract

typealias Get<IN, OUT> = (IN) -> List<OUT>

typealias Set<IN, OUT> = (List<OUT>, IN) -> IN

data class BiDiLens<IN, OUT>(val getLens: Get<IN, OUT>, val setLens: Set<IN, OUT>)

class GetLens<in IN, MID, out OUT>(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String) = { target: IN -> rootFn(name, target).map(fn) }

    fun <NEXT> map(nextFn: (OUT) -> NEXT): GetLens<IN, MID, NEXT> = GetLens(rootFn, { nextFn(fn(it)) })
}

class SetLens<IN, MID, in OUT>(private val rootFn: (String, List<MID>, IN) -> IN, private val fn: (OUT) -> MID) {
    operator fun invoke(name: String) = { values: List<OUT>, target: IN -> rootFn(name, values.map(fn), target) }
    fun <NEXT> map(nextFn: (NEXT) -> OUT): SetLens<IN, MID, NEXT> = SetLens(rootFn, { fn(nextFn(it)) })
}