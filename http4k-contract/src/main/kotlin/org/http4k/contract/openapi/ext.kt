package org.http4k.contract.openapi

import org.http4k.contract.ContractRoute
import org.http4k.contract.PathSegments
import org.http4k.lens.ParamMeta
import java.util.Locale.getDefault

fun ContractRoute.operationId(contractRoot: PathSegments) =
    meta.operationId ?: (method.name.lowercase(getDefault()) + describeFor(contractRoot)
        .split('/')
        .joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() } }
        .replace('{', '_').replace('}', '_').replace('-', '_').trimEnd('_'))

// we do this to continue to treat complex objects as strings in params
fun ParamMeta.coerceForSimpleType() = when (this) {
    is ParamMeta.ObjectParam -> ParamMeta.StringParam
    else -> this
}

