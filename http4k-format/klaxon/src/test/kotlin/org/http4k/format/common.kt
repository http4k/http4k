package org.http4k.format

import com.beust.klaxon.TypeAdapter
import com.beust.klaxon.TypeFor

open class Public
class Private : Public()

object PolymorphicAdapter : TypeAdapter<PolymorphicParent> {
    override fun classFor(type: Any) = when(type as String) {
        "first" -> FirstChild::class
        "second" -> SecondChild::class
        else -> throw IllegalArgumentException("Unknown type: $type")
    }

}
@TypeFor(field = "type", adapter = PolymorphicAdapter::class)
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
