package org.http4k.contract.openapi

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.util.JsonSchemaCreator

/**
 * Renders the contract contents in OpenApi JSON format.
 */
interface ApiRenderer<API, NODE> : JsonSchemaCreator<Any, NODE> {
    fun api(api: API): NODE

    companion object {
        /**
         * ApiRenderer which uses auto-marshalling JSON to create JSON schema for message models.
         */
        fun <T : Any, NODE : Any> Auto(json: JsonLibAutoMarshallingJson<NODE>,
                                       schema: JsonSchemaCreator<Any, NODE>): ApiRenderer<T, NODE> =
            object : ApiRenderer<T, NODE>, JsonSchemaCreator<Any, NODE> by schema {
                override fun api(api: T) = json.asJsonObject(api)
            }
    }
}

