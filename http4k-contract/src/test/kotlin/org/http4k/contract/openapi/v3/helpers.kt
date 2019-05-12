package org.http4k.contract.openapi.v3

enum class Foo {
    bar, bing
}

data class ArbObject1(val anotherString: Foo)
data class ArbObject2(val string: String, val child: ArbObject1?, val numbers: List<Int>, val bool: Boolean)
