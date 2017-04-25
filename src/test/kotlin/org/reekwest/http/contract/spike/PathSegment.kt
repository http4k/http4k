package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Get
import org.reekwest.http.contract.Lens
import org.reekwest.http.contract.LensSpec

typealias PathSegmentLens<T> = Lens<String, T>

open class PathSegmentSpec<MID, out OUT>(private val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathSegmentLens<OUT> = delegate.required(name, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSegmentSpec<MID, NEXT> = PathSegmentSpec(delegate.map(nextIn))
}

object PathSegment : PathSegmentSpec<String, String>(LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun int() = map(String::toInt)
    fun fixed(name: String) = of(name)
}
