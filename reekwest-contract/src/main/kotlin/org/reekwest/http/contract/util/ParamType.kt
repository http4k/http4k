package org.reekwest.http.contract.util

enum class ParamType(val rep: String) {
    StringParamType("string"),
    ObjectParamType("object"),
    BooleanParamType("boolean"),
    IntegerParamType("integer"),
    FileParamType("file"),
    NumberParamType("number"),
    NullParamType("null")
}