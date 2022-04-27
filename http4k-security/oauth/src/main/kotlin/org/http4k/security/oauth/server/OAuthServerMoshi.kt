package org.http4k.security.oauth.server

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token.NULL
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.format.withStandardMappings
import org.http4k.security.AccessTokenResponse
import org.http4k.security.Nonce
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State

object OAuthServerMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AccessTokenResponseAdapter)
        .add(ErrorResponseJsonAdapter)
        .asConfigurable()
        .withStandardMappings()
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType::fromQueryParameterValue, ResponseType::queryParameterValue)
        .done()
)

internal fun <T> Map<String, Any>.value(name: String, fn: Function1<String, T>) =
    this[name]?.toString()?.let(fn)

internal fun Map<*, *>.string(name: String) = this[name]?.toString()
internal fun Map<*, *>.boolean(name: String) = this[name]?.toString()?.toBoolean()
internal fun Map<*, *>.long(name: String) = this[name]?.toString()?.toBigDecimal()?.toLong()

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.map(name: String) = this[name] as Map<String, Any>?

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.strings(name: String) = this[name] as List<String>?

object AccessTokenResponseAdapter : JsonAdapter<AccessTokenResponse>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: AccessTokenResponse?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                beginObject()
                name("access_token")
                value(value.access_token)
                name("token_type")
                value(value.token_type)
                name("expires_in")
                value(value.expires_in)
                name("id_token")
                value(value.id_token)
                name("scope")
                value(value.scope)
                name("refresh_token")
                value(value.refresh_token)
                endObject()
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): AccessTokenResponse {
        val values = mutableMapOf<String, String>()
        with(reader) {
            beginObject()
            while (hasNext()) {
                when {
                    peek() != NULL -> values[nextName()] = nextSource().readUtf8().trim('"').trimEnd('"')
                    else -> skipValue()
                }
            }
            endObject()
        }

        return with(values) {
            AccessTokenResponse(
                string("access_token")!!,
                string("token_type"),
                long("expires_in"),
                string("id_token"),
                string("scope"),
                string("refresh_token")
            )
        }
    }
}

object ErrorResponseJsonAdapter : JsonAdapter<ErrorResponse>() {
    private val options = JsonReader.Options.of(
        "error",
        "error_description",
        "error_uri"
    )

    override fun toJson(writer: JsonWriter, `value`: ErrorResponse?) {
        if (`value` == null) {
            writer.nullValue()
            return
        }
        writer.beginObject()
        writer.name("error")
        writer.value(`value`.error)
        writer.name("error_description")
        writer.value(`value`.error_description)
        writer.name("error_uri")
        writer.value(`value`.error_uri)
        writer.endObject()
    }

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
