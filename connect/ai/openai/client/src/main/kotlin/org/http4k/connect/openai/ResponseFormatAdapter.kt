package org.http4k.connect.openai

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.http4k.connect.openai.action.ResponseFormat
import java.lang.reflect.Type

/**
 * OpenAI nests `name` and `strict` inside `response_format.json_schema`,
 * while `ResponseFormat.JsonSchema` exposes `strict` and `json_schema` as constructor properties
 * and stores `name` separately.
 */
object ResponseFormatAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != ResponseFormat::class.java) return null

        val mapAdapter = moshi.adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )

        return ResponseFormatJsonAdapter(mapAdapter).nullSafe()
    }
}

private class ResponseFormatJsonAdapter(
    private val mapAdapter: JsonAdapter<Map<String, Any>>
) : JsonAdapter<ResponseFormat>() {
    override fun toJson(writer: JsonWriter, value: ResponseFormat?) {
        when (value) {
            ResponseFormat.Json -> mapAdapter.toJson(writer, mapOf("type" to "json_object"))
            ResponseFormat.Url -> mapAdapter.toJson(writer, mapOf("type" to "url"))
            is ResponseFormat.JsonSchema -> {
                val jsonSchema = linkedMapOf<String, Any>(
                    "name" to value.name,
                    "schema" to value.json_schema
                )
                value.strict?.let { jsonSchema["strict"] = it }

                mapAdapter.toJson(
                    writer,
                    linkedMapOf(
                        "type" to "json_schema",
                        "json_schema" to jsonSchema
                    )
                )
            }

            null -> writer.nullValue()
        }
    }

    override fun fromJson(reader: JsonReader): ResponseFormat {
        val raw = mapAdapter.fromJson(reader) ?: throw JsonDataException("Expected response format")
        return when (val type = raw["type"] as? String) {
            "json_object" -> ResponseFormat.Json
            "url" -> ResponseFormat.Url
            "json_schema" -> raw.toJsonSchema()
            else -> throw JsonDataException("Unknown response format type: $type")
        }
    }

    private fun Map<String, Any>.toJsonSchema(): ResponseFormat.JsonSchema {
        val topLevelStrict = this["strict"] as? Boolean
        val jsonSchema = (this["json_schema"] as? Map<*, *>)?.asStringAnyMap()
            ?: throw JsonDataException("json_schema response format requires a json_schema object")

        val nestedSchema = (jsonSchema["schema"] as? Map<*, *>)?.asStringAnyMap()

        return when (nestedSchema) {
            null -> ResponseFormat.JsonSchema(
                strict = topLevelStrict,
                json_schema = jsonSchema
            )

            else -> ResponseFormat.JsonSchema(
                name = jsonSchema["name"] as? String ?: "response",
                strict = jsonSchema["strict"] as? Boolean ?: topLevelStrict,
                json_schema = nestedSchema
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<*, *>.asStringAnyMap(): Map<String, Any> = this.mapKeys { (key) ->
        key as? String ?: throw JsonDataException("Expected string key in response format")
    } as Map<String, Any>
}
