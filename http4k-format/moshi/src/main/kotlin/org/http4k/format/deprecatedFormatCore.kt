package org.http4k.format

@Deprecated("Renamed", ReplaceWith("MapAdapter"))
object CollectionEdgeCasesAdapter : IsAnInstanceOfAdapter<Map<*, *>>(Map::class)
