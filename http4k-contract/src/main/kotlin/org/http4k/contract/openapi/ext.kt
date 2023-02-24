package org.http4k.contract.openapi

import org.http4k.contract.RouteMeta
import org.http4k.core.Method
import java.util.Locale.getDefault

fun operationId(routeMeta: RouteMeta, method: Method, description: String) =
    routeMeta.operationId ?: (method.name.lowercase(getDefault()) + description
        .split('/')
        .joinToString("") { it.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() } }
        .replace('{', '_').replace('}', '_').replace('-', '_').trimEnd('_'))
