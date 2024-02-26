package org.http4k.lens

import org.http4k.core.Body
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.format.Moshi.auto
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

class Foo(request: Request) : TypedRequest(request) {
    val asd by required(Path)
    val hade by required(Header)
    val boasd by body(Body.auto<MyType>())
}

fun main() {
    val kClass = Foo::class
    println(kClass.metas())
}
