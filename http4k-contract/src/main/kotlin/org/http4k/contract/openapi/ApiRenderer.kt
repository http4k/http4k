package org.http4k.contract.openapi

import org.http4k.contract.openapi.v3.AutoJsonToJsonSchema
import org.http4k.contract.openapi.v3.JsonToJsonSchema
import org.http4k.format.AutoMarshallingJson
import org.http4k.util.JsonSchema
import org.http4k.util.JsonSchemaCreator
import java.util.concurrent.atomic.AtomicReference

/**
 * Renders the contract contents in OpenApi JSON format.
 */
interface ApiRenderer<API, NODE> : JsonSchemaCreator<Any, NODE> {
    fun api(api: API): NODE

    companion object {
        /**
         * ApiRenderer which uses auto-marshalling JSON to create JSON schema for message models.
         */
        fun <T : Any, NODE : Any> Auto(
            json: AutoMarshallingJson<NODE>,
            schema: JsonSchemaCreator<Any, NODE> = AutoJsonToJsonSchema(json)): ApiRenderer<T, NODE> {
            val fallbackSchema = object : JsonSchemaCreator<Any, NODE> {
                private val jsonNodes = JsonToJsonSchema(json)
                override fun toSchema(obj: Any, overrideDefinitionId: String?, prefix: String?): JsonSchema<NODE> =
                    try {
                        @Suppress("UNCHECKED_CAST")
                        jsonNodes.toSchema(obj as NODE, overrideDefinitionId, null)
                    } catch (e: ClassCastException) {
                        schema.toSchema(obj, overrideDefinitionId, null)
                    }
            }

            return object : ApiRenderer<T, NODE>, JsonSchemaCreator<Any, NODE> by fallbackSchema {
                override fun api(api: T) = json.asJsonObject(api)
            }
        }
    }
}

/**
 * Cache the result of the API render, in case it is expensive to calculate.
 */
fun <API : Any, NODE : Any> ApiRenderer<API, NODE>.cached(): ApiRenderer<API, NODE> = object : ApiRenderer<API, NODE> by this {
    private val cached = AtomicReference<NODE>()
    override fun api(api: API): NODE = cached.get() ?: this@cached.api(api).also { cached.set(it) }
}
