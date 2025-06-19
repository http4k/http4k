package org.http4k.ai.mcp.util

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.jsonrpc.ErrorMessage

object ErrorMessageAdapter : JsonAdapter<ErrorMessage>() {

    @FromJson
    override fun fromJson(reader: JsonReader): ErrorMessage {
        reader.beginObject()
        var code = -1
        var message = ""

        while (reader.hasNext()) {
            when (reader.nextName()) {
                "code" -> code = reader.nextInt()
                "message" -> message = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return ErrorMessage(code, message)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: ErrorMessage?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        writer.name("code").value(value.code)
        writer.name("message").value(value.message)
        writer.endObject()
    }
}
