package org.http4k.connect.ollama

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.reflect.Type

object ResponseFormatAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (!ResponseFormat::class.java.isAssignableFrom(Types.getRawType(type))) return null
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
            is ResponseFormat.json -> writer.value("json")
            is ResponseFormat.Schema -> mapAdapter.toJson(writer, value.schema)
            null -> writer.nullValue()
        }
    }

    override fun fromJson(reader: JsonReader): ResponseFormat {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> {
                when (val str = reader.nextString()) {
                    "json" -> ResponseFormat.json
                    else -> throw JsonDataException("Unknown response format: $str")
                }
            }
            JsonReader.Token.BEGIN_OBJECT -> {
                ResponseFormat.Schema(mapAdapter.fromJson(reader)!!)
            }
            else -> throw JsonDataException("Expected string or object for format")
        }
    }
}
