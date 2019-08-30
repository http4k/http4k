package org.http4k.contract.openapi

import org.http4k.contract.ContractRoute
import org.http4k.contract.PathSegments

fun ContractRoute.operationId(contractRoot: PathSegments) =
    meta.operationId ?: method.name.toLowerCase() + describeFor(contractRoot)
        .split('/').joinToString("") { it.capitalize() }.replace('{', '_').replace('}', '_').trimEnd('_')
