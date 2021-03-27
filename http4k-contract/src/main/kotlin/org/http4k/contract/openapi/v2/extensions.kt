package org.http4k.contract.openapi.v2

import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NullParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam

val ParamMeta.value
    get() = when (this) {
        is ArrayParam -> "array"
        StringParam -> "string"
        ObjectParam -> "object"
        BooleanParam -> "boolean"
        IntegerParam -> "integer"
        FileParam -> "file"
        NumberParam -> "number"
        NullParam -> "null"
    }
