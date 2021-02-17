package org.http4k.lens

sealed class ParamMeta(val description: String) {
    data class ArrayParam(private val itemType: ParamMeta) : ParamMeta("array") {
        fun itemType() = itemType
    }
    object StringParam : ParamMeta("string")
    object ObjectParam : ParamMeta("object")
    object BooleanParam : ParamMeta("boolean")
    object IntegerParam : ParamMeta("integer")
    object FileParam : ParamMeta("file")
    object NumberParam : ParamMeta("number")
    object NullParam : ParamMeta("null")
}
