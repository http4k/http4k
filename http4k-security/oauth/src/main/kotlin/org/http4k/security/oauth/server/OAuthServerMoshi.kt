package org.http4k.security.oauth.server

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
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
import org.http4k.security.oauth.core.ClientId

internal object OAuthServerMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(AccessTokenResponseAdapter)
        .asConfigurable()
        .withStandardMappings()
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode.Companion::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue)
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
