package org.reekwest.http.contract

class MappableGetLens<in IN, MID, out OUT>(private val rootFn: (String, IN) -> List<MID>, private val fn: (MID) -> OUT) {
    operator fun invoke(name: String): GetLens<IN, OUT> = object : GetLens<IN, OUT> {
        override fun invoke(target: IN): List<OUT> = rootFn(name, target).map(fn)
    }

    fun <NEXT> map(nextFn: (OUT) -> NEXT): MappableGetLens<IN, MID, NEXT> = MappableGetLens(rootFn, { nextFn(fn(it)) })
}