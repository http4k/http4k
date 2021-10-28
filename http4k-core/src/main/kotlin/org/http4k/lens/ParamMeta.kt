package org.http4k.lens

import kotlin.reflect.KClass

sealed class ParamMeta(val description: String) {
    data class ArrayParam(private val itemType: ParamMeta) : ParamMeta("array") {
        fun itemType() = itemType
    }
    class EnumParam<T : Enum<T>>(val clz: KClass<T>) : ParamMeta("string")
    object StringParam : ParamMeta("string")
    object ObjectParam : ParamMeta("object")
    object BooleanParam : ParamMeta("boolean")
    object IntegerParam : ParamMeta("integer")
    object FileParam : ParamMeta("file")
    object NumberParam : ParamMeta("number")
    object NullParam : ParamMeta("null")
}
