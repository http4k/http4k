package org.http4k.contract.openapi

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.util.JsonSchemaCreator

interface ApiRenderer<API, NODE> : JsonSchemaCreator<API, NODE> {
    fun api(api: API): NODE

    companion object {
        fun <T : Any, NODE : Any> Auto(json: JsonLibAutoMarshallingJson<NODE>,
                                       schema: JsonSchemaCreator<T, NODE>): ApiRenderer<T, NODE> =
            object : ApiRenderer<T, NODE>, JsonSchemaCreator<T, NODE> by schema {
                override fun api(api: T) = json.asJsonObject(api)
            }
    }
}

