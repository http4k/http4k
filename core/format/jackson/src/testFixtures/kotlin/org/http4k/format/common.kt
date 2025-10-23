package org.http4k.format

import tools.jackson.annotation.JsonSubTypes
import tools.jackson.annotation.JsonTypeInfo
import tools.jackson.annotation.JsonView

open class Public
class Private : Public()

data class ArbObjectWithView(@JsonView(Private::class) @JvmField val priv: Int = 0, @JsonView(Public::class) @JvmField val pub: Int)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@class")
@JsonSubTypes(
    JsonSubTypes.Type(value = FirstChild::class, name = "first"),
    JsonSubTypes.Type(value = SecondChild::class, name = "second")
)
sealed class PolymorphicParent

data class FirstChild(val something: String) : PolymorphicParent()
data class SecondChild(val somethingElse: String) : PolymorphicParent()

abstract class NotSealedParent
data class NonSealedChild(val something: String) : NotSealedParent()

interface Interface {
    val value: String
}

class InterfaceImpl : Interface {
    override val value = "hello"
    val subValue = "123"
}
