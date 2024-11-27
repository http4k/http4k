package org.http4k.security.oauth.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.format.number
import org.http4k.format.obj
import org.http4k.format.string
import org.http4k.format.stringOrNull
import org.http4k.security.AccessTokenResponse

object AccessTokenResponseMoshiAdapter : JsonAdapter<AccessTokenResponse>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: AccessTokenResponse?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                obj(value) {
                    string("access_token", access_token)
                    string("token_type", token_type)
                    number("expires_in", expires_in)
                    string("id_token", id_token)
                    string("scope", scope)
                    string("refresh_token", refresh_token)
                }
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): AccessTokenResponse {
        val values = mutableMapOf<String, String?>()
        with(reader) {
            beginObject()
            while (hasNext()) {
                values[nextName()] = stringOrNull()
            }
            endObject()
        }

        return with(values) {
            AccessTokenResponse(
                string("access_token") ?: throw JsonDataException("access_token was null"),
                string("token_type"),
                long("expires_in"),
                string("id_token"),
                string("scope"),
                string("refresh_token")
            )
        }
    }
}
