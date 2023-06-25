package org.http4k.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token.NULL
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.security.oauth.server.ErrorResponse

object ErrorResponseMoshi : JsonAdapter<ErrorResponse>() {
    private val options = JsonReader.Options.of(
        "error",
        "error_description",
        "error_uri"
    )

    @ToJson
    override fun toJson(writer: JsonWriter, value: ErrorResponse?) {
        with(writer) {
            obj(value) {
                string("error", error)
                string("error_description", error_description)
                string("error_uri", error_uri)
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ErrorResponse? {
        if (reader.peek() == NULL) return reader.nextNull()

        var error: String? = null
        var error_description: String? = null
        var error_uri: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.selectName(options)) {
                0 -> {
                    if (reader.peek() == NULL) reader.skipValue()
                    else error = reader.nextString()
                }
                1 -> {
                    if (reader.peek() == NULL) reader.skipValue() else error_description = reader.nextString()
                }
                2 -> {
                    if (reader.peek() == NULL) reader.skipValue() else error_uri = reader.nextString()
                }
                -1 -> {
                    reader.skipName()
                    reader.skipValue()
                }
            }
        }
        reader.endObject()

        if (error == null) throw JsonDataException("error was null")
        if (error_description == null) throw JsonDataException("error_description was null")

        return ErrorResponse(error, error_description, error_uri)
    }
}
