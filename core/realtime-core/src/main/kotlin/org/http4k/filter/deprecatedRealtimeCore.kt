package org.http4k.filter

@Deprecated("use PolyFilters.CorsAndRebindProtection instead", ReplaceWith("PolyFilters.CorsAndRebindProtection"))
fun ServerFilters.CorsAndRebindProtection(corsPolicy: CorsPolicy) = PolyFilters.CorsAndRebindProtection(corsPolicy)
