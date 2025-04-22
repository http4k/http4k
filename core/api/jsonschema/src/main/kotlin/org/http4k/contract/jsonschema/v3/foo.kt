package org.http4k.contract.jsonschema.v3

import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType

enum class Foo {
    a, b
}

data class FooBar(val a : String, val b: List<String>, val c: Foo)

fun main() {

    FooBar::class.memberProperties.map {
        println(when {
            it.returnType.isSubtypeOf(Iterable::class.starProjectedType) -> it.name + " is iterable"
            it.returnType.isSubtypeOf(Enum::class.starProjectedType) -> it.name + " is enum"
            else -> it.name + " is object"
        }
        )
    }
}
