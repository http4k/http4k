package org.http4k.lens

sealed class ParamMeta(val description: String) {
    object ArrayParam : ParamMeta("array")
    object StringParam : ParamMeta("string")
    object ObjectParam : ParamMeta("object")
    object BooleanParam : ParamMeta("boolean")
    object IntegerParam : ParamMeta("integer")
    object FileParam : ParamMeta("file")
    object NumberParam : ParamMeta("number")
    object NullParam : ParamMeta("null")
}
