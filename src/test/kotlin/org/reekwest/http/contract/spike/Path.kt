package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Get
import org.reekwest.http.contract.Lens
import org.reekwest.http.contract.LensSpec

typealias PathLens<T> = Lens<String, T>

open class PathSpec<MID, out OUT>(private val delegate: LensSpec<String, String, OUT>) {
    open fun of(name: String, description: String? = null): PathLens<OUT> = delegate.required(name, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))
}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun int() = map(String::toInt)
    fun fixed(name: String) = of(name)
}
