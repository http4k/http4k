package org.http4k.security.oauth.format

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import org.http4k.core.Uri
import org.http4k.format.list
import org.http4k.format.obj
import org.http4k.format.string
import org.http4k.security.ResponseType
import org.http4k.security.oauth.metadata.AuthMethod
import org.http4k.security.oauth.metadata.ServerMetadata
import java.util.Locale

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
        return with(reader) {
            beginObject()
            var issuer: String? = null
            var authorizationEndpoint: Uri? = null
            var tokenEndpoint: Uri? = null
            var tokenEndpointAuthMethods: List<AuthMethod> = emptyList()
            var tokenEndpointAuthSigningAlgs: List<String> = emptyList()
            var responseTypesSupported: List<ResponseType> = emptyList()
            var scopesSupported: List<String> = emptyList()
            var uiLocalesSupported: List<Locale>? = null
            var userinfoEndpoint: Uri? = null
            var jwksUri: Uri? = null
            var registrationEndpoint: Uri? = null
            var serviceDocumentation: Uri? = null
            var signedMetadata: String? = null

            while (hasNext()) {
                when (nextName()) {
                    "issuer" -> issuer = nextString()
                    "authorization_endpoint" -> authorizationEndpoint = Uri.of(nextString())
                    "token_endpoint" -> tokenEndpoint = Uri.of(nextString())
                    "token_endpoint_auth_methods_supported" -> tokenEndpointAuthMethods =
                        readStringArray().map { AuthMethod.valueOf(it) }

                    "token_endpoint_auth_signing_alg_values_supported" -> tokenEndpointAuthSigningAlgs =
                        readStringArray().toList()

                    "response_types_supported" -> responseTypesSupported =
                        readStringArray().map { ResponseType.valueOf(it) }

                    "scopes_supported" -> scopesSupported = readStringArray().toList()
                    "ui_locales_supported" -> uiLocalesSupported =
                        readStringArray().map { Locale.forLanguageTag(it) }

                    "userinfo_endpoint" -> userinfoEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "jwks_uri" -> jwksUri = nextStringOrNull()?.let { Uri.of(it) }
                    "registration_endpoint" -> registrationEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "service_documentation" -> serviceDocumentation = nextStringOrNull()?.let { Uri.of(it) }
                    "signed_metadata" -> signedMetadata = nextStringOrNull()
                    else -> skipValue()
                }
            }
            endObject()

            ServerMetadata(
                issuer = requireNotNull(issuer) { "issuer is required" },
                authorization_endpoint = requireNotNull(authorizationEndpoint) { "authorization_endpoint is required" },
                token_endpoint = requireNotNull(tokenEndpoint) { "token_endpoint is required" },
                token_endpoint_auth_methods_supported = tokenEndpointAuthMethods,
                token_endpoint_auth_signing_alg_values_supported = tokenEndpointAuthSigningAlgs,
                response_types_supported = responseTypesSupported,
                scopes_supported = scopesSupported,
                ui_locales_supported = uiLocalesSupported,
                userinfo_endpoint = userinfoEndpoint,
                jwks_uri = jwksUri,
                registration_endpoint = registrationEndpoint,
                service_documentation = serviceDocumentation,
                signed_metadata = signedMetadata
            )
        }
    }

    private fun JsonReader.readStringArray(): Array<String> {
        val result = mutableListOf<String>()
        beginArray()
        while (hasNext()) {
            result.add(nextString())
        }
        endArray()
        return result.toTypedArray()
    }

    private fun JsonReader.nextStringOrNull(): String? =
        if (peek() == JsonReader.Token.NULL) {
            nextNull<String>()
            null
        } else {
            nextString()
        }
}
