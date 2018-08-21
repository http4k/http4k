package org.http4k.jsonrpc

import org.http4k.format.Json

interface MethodMappingsBuilder<ROOT: NODE, NODE> {
    fun mappings(): List<MethodMapping<NODE, NODE>>
    fun method(name: String, handler: RequestHandler<NODE, NODE>)
    val json: Json<ROOT, NODE>
}