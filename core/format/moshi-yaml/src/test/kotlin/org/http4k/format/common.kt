package org.http4k.format

open class Public

abstract class NotSealedParent

interface Interface {
    val value: String
}

class InterfaceImpl : Interface {
    override val value = "hello"
    val subValue = "123"
}
