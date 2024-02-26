package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.format.Moshi.auto

class Foo(request: Request) : TypedRequest(request) {
    val asd by required(Path)
    val hade by required(Header.int())
    val boasd by body(Body.auto<MyType>(), MyType("hello"))
}

fun main() {
    val kClass = Foo::class
    println(kClass.routeParams())
}
