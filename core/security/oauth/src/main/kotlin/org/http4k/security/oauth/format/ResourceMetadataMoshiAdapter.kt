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
import org.http4k.security.oauth.metadata.BearerMethod
import org.http4k.security.oauth.metadata.ResourceMetadata

object ResourceMetadataMoshiAdapter : JsonAdapter<ResourceMetadata>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: ResourceMetadata?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                obj(value) {
                    string("resource", resource.toString())
                    list("authorization_servers", authorizationServers?.map { it.toString() })
                    string("jwks_uri", jwksUri?.toString())
                    list("scopes_supported", scopesSupported)
                    list("bearer_methods_supported", bearerMethodsSupported?.map { it.name.lowercase() })
                    list("resource_signing_alg_values_supported", resourceSigningAlgValuesSupported)
                    string("resource_name", resourceName)
                    string("resource_documentation", resourceDocumentation?.toString())
                    string("resource_policy_uri", resourcePolicyUri?.toString())
                    string("resource_tos_uri", resourceTosUri?.toString())
                    tlsClientCertificateBoundAccessTokens?.let { writer.name("tls_client_certificate_bound_access_tokens").value(it) }
                    list("authorization_details_types_supported", authorizationDetailsTypesSupported)
                    list("dpop_signing_alg_values_supported", dpopSigningAlgValuesSupported)
                    dpopBoundAccessTokensRequired?.let { writer.name("dpop_bound_access_tokens_required").value(it) }
                    string("signed_metadata", signedMetadata)
                }
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): ResourceMetadata {
        return with(reader) {
            beginObject()
            var resource: Uri? = null
            var authorizationServers: List<Uri>? = null
            var jwksUri: Uri? = null
            var scopesSupported: List<String>? = null
            var bearerMethodsSupported: List<BearerMethod>? = null
            var resourceSigningAlgValuesSupported: List<String>? = null
            var resourceName: String? = null
            var resourceDocumentation: Uri? = null
            var resourcePolicyUri: Uri? = null
            var resourceTosUri: Uri? = null
            var tlsClientCertificateBoundAccessTokens: Boolean? = null
            var authorizationDetailsTypesSupported: List<String>? = null
            var dpopSigningAlgValuesSupported: List<String>? = null
            var dpopBoundAccessTokensRequired: Boolean? = null
            var signedMetadata: String? = null

            while (hasNext()) {
                when (nextName()) {
                    "resource" -> resource = Uri.of(nextString())
                    "authorization_servers" -> authorizationServers = readUriArray()
                    "jwks_uri" -> jwksUri = nextStringOrNull()?.let { Uri.of(it) }
                    "scopes_supported" -> scopesSupported = readStringArray().toList()
                    "bearer_methods_supported" -> bearerMethodsSupported =
                        readStringArray().map { BearerMethod.valueOf(it.uppercase()) }
                    "resource_signing_alg_values_supported" -> resourceSigningAlgValuesSupported =
                        readStringArray().toList()
                    "resource_name" -> resourceName = nextStringOrNull()
                    "resource_documentation" -> resourceDocumentation = nextStringOrNull()?.let { Uri.of(it) }
                    "resource_policy_uri" -> resourcePolicyUri = nextStringOrNull()?.let { Uri.of(it) }
                    "resource_tos_uri" -> resourceTosUri = nextStringOrNull()?.let { Uri.of(it) }
                    "tls_client_certificate_bound_access_tokens" -> tlsClientCertificateBoundAccessTokens = nextBoolean()
                    "authorization_details_types_supported" -> authorizationDetailsTypesSupported =
                        readStringArray().toList()
                    "dpop_signing_alg_values_supported" -> dpopSigningAlgValuesSupported =
                        readStringArray().toList()
                    "dpop_bound_access_tokens_required" -> dpopBoundAccessTokensRequired = nextBoolean()
                    "signed_metadata" -> signedMetadata = nextStringOrNull()
                    else -> skipValue()
                }
            }
            endObject()

            ResourceMetadata(
                resource = requireNotNull(resource) { "resource is required" },
                authorizationServers = authorizationServers,
                jwksUri = jwksUri,
                scopesSupported = scopesSupported,
                bearerMethodsSupported = bearerMethodsSupported,
                resourceSigningAlgValuesSupported = resourceSigningAlgValuesSupported,
                resourceName = resourceName,
                resourceDocumentation = resourceDocumentation,
                resourcePolicyUri = resourcePolicyUri,
                resourceTosUri = resourceTosUri,
                tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens,
                authorizationDetailsTypesSupported = authorizationDetailsTypesSupported,
                dpopSigningAlgValuesSupported = dpopSigningAlgValuesSupported,
                dpopBoundAccessTokensRequired = dpopBoundAccessTokensRequired,
                signedMetadata = signedMetadata
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

    private fun JsonReader.readUriArray(): List<Uri> {
        val result = mutableListOf<Uri>()
        beginArray()
        while (hasNext()) {
            result.add(Uri.of(nextString()))
        }
        endArray()
        return result
    }

    private fun JsonReader.nextStringOrNull(): String? =
        if (peek() == JsonReader.Token.NULL) {
            nextNull<String>()
            null
        } else {
            nextString()
        }
}
