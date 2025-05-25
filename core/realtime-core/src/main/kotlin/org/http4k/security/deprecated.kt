package org.http4k.security

import org.http4k.core.PolyHandler
import org.http4k.core.then
import org.http4k.filter.ServerFilters

@Deprecated("use ServerFilters.PolySecurity instead", ReplaceWith("ServerFilters.PolySecurity(this).then(poly)"))
suspend fun Security.then(poly: PolyHandler) = ServerFilters.PolySecurity(this).then(poly)

