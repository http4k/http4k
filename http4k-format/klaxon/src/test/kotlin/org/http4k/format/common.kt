package org.http4k.format

abstract class NotSealedParent
data class NonSealedChild(val something: String) : NotSealedParent()

interface Interface {
    val value: String
}

class InterfaceImpl : Interface {
    override val value = "hello"
    val subValue = "123"
}
