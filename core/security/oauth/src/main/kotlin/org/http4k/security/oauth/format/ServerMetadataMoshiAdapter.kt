package org.http4k.security.oauth.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.format.list
import org.http4k.format.obj
import org.http4k.format.string
import org.http4k.security.oauth.metadata.ServerMetadata

object ServerMetadataMoshiAdapter : JsonAdapter<ServerMetadata>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: ServerMetadata?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                obj(value) {
                    string("issuer", issuer)
                    string("authorization_endpoint", authorization_endpoint.toString())
                    string("token_endpoint", token_endpoint.toString())
                    list("token_endpoint_auth_methods_supported", token_endpoint_auth_methods_supported.map { it.name })
                    list(
                        "token_endpoint_auth_signing_alg_values_supported",
                        token_endpoint_auth_signing_alg_values_supported
                    )
                    list("response_types_supported", response_types_supported.map { it.name })
                    list("scopes_supported", scopes_supported)
                    ui_locales_supported?.also { list("ui_locales_supported", it.map { it.toLanguageTag() }) }
                    string("userinfo_endpoint", userinfo_endpoint?.toString())
                    string("jwks_uri", jwks_uri?.toString())
                    string("registration_endpoint", registration_endpoint?.toString())
                    string("service_documentation", service_documentation?.toString())
                    string("signed_metadata", signed_metadata)
                }
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ServerMetadata {
        throw UnsupportedOperationException()
    }
}
