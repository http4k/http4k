package org.http4k.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.core.Uri
import org.http4k.security.oauth.client.TokenRequest

object TokenRequestMoshi : JsonAdapter<TokenRequest>() {
    @FromJson
    override fun fromJson(reader: JsonReader) =
        with(reader) {
            beginObject()
            val values = buildMap<String, String> {
                while(hasNext()) {
                    when {
                        peek() != JsonReader.Token.NULL -> put(nextName(), nextSource().readUtf8().trim('"').trimEnd('"'))
                        else -> skipValue()
                    }
                }
            }
            endObject()

            TokenRequest(
                grant_type = values["grant_type"] ?: throw JsonDataException("grant_type was null"),
                refresh_token = values["refresh_token"],
                client_id = values["client_id"],
                code = values["code"],
                redirect_uri = values["redirect_uri"]?.let(Uri::of)
            )
        }

    @ToJson
    override fun toJson(writer: JsonWriter, request: TokenRequest?) {
        with(writer) {
            obj(request) {
                string("grant_type", grant_type)
                string("refresh_token", refresh_token)
                string("client_id", client_id)
                string("code", code)
                string("redirect_uri", redirect_uri?.toString())
            }
        }
    }
}
