package org.reekwest.http.contract.spike

import org.reekwest.http.contract.Get
import org.reekwest.http.contract.LensSpec

open class PathSpec<MID, out OUT>(private val delegate: LensSpec<String, MID, OUT>) {
    open fun of(name: String, description: String? = null) = delegate.required(name, description)
    fun <NEXT> map(nextIn: (OUT) -> NEXT): PathSpec<MID, NEXT> = PathSpec(delegate.map(nextIn))
}

object Path : PathSpec<String, String>(LensSpec<String, String, String>("path",
    Get { _, target -> listOf(target) })) {

    fun int() = map(String::toInt)
}

fun main(args: Array<String>) {
    val a = Path.int().of("name")
    a("123")
}