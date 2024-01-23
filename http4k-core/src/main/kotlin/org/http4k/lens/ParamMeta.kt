package org.http4k.lens

import kotlin.reflect.KClass

sealed class ParamMeta(val description: String) {
    data class ArrayParam(private val itemType: ParamMeta) : ParamMeta("array") {
        fun itemType() = itemType
    }

    class EnumParam<T : Enum<T>>(val clz: KClass<T>) : ParamMeta("string")
    data object StringParam : ParamMeta("string")
    data object ObjectParam : ParamMeta("object")
    data object BooleanParam : ParamMeta("boolean")
    data object IntegerParam : ParamMeta("integer")
    data object FileParam : ParamMeta("file")
    data object NumberParam : ParamMeta("number")
    data object NullParam : ParamMeta("null")
}
