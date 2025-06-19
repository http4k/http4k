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
import org.http4k.security.ResponseType.Companion.fromQueryParameterValue
import org.http4k.security.oauth.metadata.OpenIdConfiguration

object OpenIdConfigurationMoshiAdapter : JsonAdapter<OpenIdConfiguration>() {
    @ToJson
    override fun toJson(writer: JsonWriter, value: OpenIdConfiguration?) {
        when (value) {
            null -> writer.nullValue()
            else -> with(writer) {
                obj(value) {
                    string("issuer", value.issuer.toString())
                    string("authorization_endpoint", value.authorizationEndpoint.toString())
                    string("token_endpoint", value.tokenEndpoint.toString())
                    string("jwks_uri", value.jwksUri.toString())
                    list("response_types_supported", value.responseTypesSupported.map { it.queryParameterValue })
                    list("subject_types_supported", value.subjectTypesSupported)
                    list("id_token_signing_alg_values_supported", value.idTokenSigningAlgValuesSupported)

                    value.userinfoEndpoint?.let { string("userinfo_endpoint", it.toString()) }
                    value.registrationEndpoint?.let { string("registration_endpoint", it.toString()) }
                    value.scopesSupported?.let { list("scopes_supported", it) }
                    value.claimsSupported?.let { list("claims_supported", it) }
                    value.grantTypesSupported?.let { list("grant_types_supported", it) }
                    value.tokenEndpointAuthMethodsSupported?.let { list("token_endpoint_auth_methods_supported", it) }
                    value.tokenEndpointAuthSigningAlgValuesSupported?.let {
                        list(
                            "token_endpoint_auth_signing_alg_values_supported",
                            it
                        )
                    }
                    value.serviceDocumentation?.let { string("service_documentation", it.toString()) }
                    value.uiLocalesSupported?.let { list("ui_locales_supported", it) }

                    value.endSessionEndpoint?.let { string("end_session_endpoint", it.toString()) }
                    value.checkSessionIframe?.let { string("check_session_iframe", it.toString()) }

                    value.revocationEndpoint?.let { string("revocation_endpoint", it.toString()) }
                    value.introspectionEndpoint?.let { string("introspection_endpoint", it.toString()) }
                    value.claimsParameterSupported?.let { writer.name("claims_parameter_supported").value(it) }
                    value.requestParameterSupported?.let { writer.name("request_parameter_supported").value(it) }
                    value.requestUriParameterSupported?.let { writer.name("request_uri_parameter_supported").value(it) }
                    value.requireRequestUriRegistration?.let {
                        writer.name("require_request_uri_registration").value(it)
                    }
                    value.opPolicyUri?.let { string("op_policy_uri", it.toString()) }
                    value.opTosUri?.let { string("op_tos_uri", it.toString()) }
                    value.codeChallengeMethodsSupported?.let { list("code_challenge_methods_supported", it) }
                    value.idTokenEncryptionAlgValuesSupported?.let {
                        list(
                            "id_token_encryption_alg_values_supported",
                            it
                        )
                    }
                    value.idTokenEncryptionEncValuesSupported?.let {
                        list(
                            "id_token_encryption_enc_values_supported",
                            it
                        )
                    }
                    value.userinfoSigningAlgValuesSupported?.let { list("userinfo_signing_alg_values_supported", it) }
                    value.userinfoEncryptionAlgValuesSupported?.let {
                        list(
                            "userinfo_encryption_alg_values_supported",
                            it
                        )
                    }
                    value.userinfoEncryptionEncValuesSupported?.let {
                        list(
                            "userinfo_encryption_enc_values_supported",
                            it
                        )
                    }
                    value.requestObjectSigningAlgValuesSupported?.let {
                        list(
                            "request_object_signing_alg_values_supported",
                            it
                        )
                    }
                    value.requestObjectEncryptionAlgValuesSupported?.let {
                        list(
                            "request_object_encryption_alg_values_supported",
                            it
                        )
                    }
                    value.requestObjectEncryptionEncValuesSupported?.let {
                        list(
                            "request_object_encryption_enc_values_supported",
                            it
                        )
                    }
                    value.backchannelLogoutSupported?.let { writer.name("backchannel_logout_supported").value(it) }
                    value.backchannelLogoutSessionSupported?.let {
                        writer.name("backchannel_logout_session_supported").value(it)
                    }
                }
            }
        }
    }

    @FromJson
    override fun fromJson(reader: JsonReader): OpenIdConfiguration {
        return with(reader) {
            beginObject()
            var issuer: Uri? = null
            var authorizationEndpoint: Uri? = null
            var tokenEndpoint: Uri? = null
            var jwksUri: Uri? = null
            var responseTypesSupported: List<ResponseType>? = null
            var subjectTypesSupported: List<String>? = null
            var idTokenSigningAlgValuesSupported: List<String>? = null

            var userinfoEndpoint: Uri? = null
            var registrationEndpoint: Uri? = null
            var scopesSupported: List<String>? = null
            var claimsSupported: List<String>? = null
            var grantTypesSupported: List<String>? = null
            var tokenEndpointAuthMethodsSupported: List<String>? = null
            var tokenEndpointAuthSigningAlgValuesSupported: List<String>? = null
            var serviceDocumentation: Uri? = null
            var uiLocalesSupported: List<String>? = null

            var endSessionEndpoint: Uri? = null
            var checkSessionIframe: Uri? = null

            var revocationEndpoint: Uri? = null
            var introspectionEndpoint: Uri? = null
            var claimsParameterSupported: Boolean? = null
            var requestParameterSupported: Boolean? = null
            var requestUriParameterSupported: Boolean? = null
            var requireRequestUriRegistration: Boolean? = null
            var opPolicyUri: Uri? = null
            var opTosUri: Uri? = null
            var codeChallengeMethodsSupported: List<String>? = null
            var idTokenEncryptionAlgValuesSupported: List<String>? = null
            var idTokenEncryptionEncValuesSupported: List<String>? = null
            var userinfoSigningAlgValuesSupported: List<String>? = null
            var userinfoEncryptionAlgValuesSupported: List<String>? = null
            var userinfoEncryptionEncValuesSupported: List<String>? = null
            var requestObjectSigningAlgValuesSupported: List<String>? = null
            var requestObjectEncryptionAlgValuesSupported: List<String>? = null
            var requestObjectEncryptionEncValuesSupported: List<String>? = null
            var backchannelLogoutSupported: Boolean? = null
            var backchannelLogoutSessionSupported: Boolean? = null

            val additional = mutableMapOf<String, Any?>()

            while (hasNext()) {
                when (val name = nextName()) {
                    "issuer" -> issuer = Uri.of(nextString())
                    "authorization_endpoint" -> authorizationEndpoint = Uri.of(nextString())
                    "token_endpoint" -> tokenEndpoint = Uri.of(nextString())
                    "jwks_uri" -> jwksUri = Uri.of(nextString())
                    "response_types_supported" -> responseTypesSupported =
                        readStringArray().toList().map { fromQueryParameterValue(it) }

                    "subject_types_supported" -> subjectTypesSupported = readStringArray().toList()
                    "id_token_signing_alg_values_supported" -> idTokenSigningAlgValuesSupported =
                        readStringArray().toList()

                    "userinfo_endpoint" -> userinfoEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "registration_endpoint" -> registrationEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "scopes_supported" -> scopesSupported = readStringArray().toList()
                    "claims_supported" -> claimsSupported = readStringArray().toList()
                    "grant_types_supported" -> grantTypesSupported = readStringArray().toList()
                    "token_endpoint_auth_methods_supported" -> tokenEndpointAuthMethodsSupported =
                        readStringArray().toList()

                    "token_endpoint_auth_signing_alg_values_supported" -> tokenEndpointAuthSigningAlgValuesSupported =
                        readStringArray().toList()

                    "service_documentation" -> serviceDocumentation = nextStringOrNull()?.let { Uri.of(it) }
                    "ui_locales_supported" -> uiLocalesSupported = readStringArray().toList()

                    "end_session_endpoint" -> endSessionEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "check_session_iframe" -> checkSessionIframe = nextStringOrNull()?.let { Uri.of(it) }

                    "revocation_endpoint" -> revocationEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "introspection_endpoint" -> introspectionEndpoint = nextStringOrNull()?.let { Uri.of(it) }
                    "claims_parameter_supported" -> claimsParameterSupported = nextBoolean()
                    "request_parameter_supported" -> requestParameterSupported = nextBoolean()
                    "request_uri_parameter_supported" -> requestUriParameterSupported = nextBoolean()
                    "require_request_uri_registration" -> requireRequestUriRegistration = nextBoolean()
                    "op_policy_uri" -> opPolicyUri = nextStringOrNull()?.let { Uri.of(it) }
                    "op_tos_uri" -> opTosUri = nextStringOrNull()?.let { Uri.of(it) }
                    "code_challenge_methods_supported" -> codeChallengeMethodsSupported = readStringArray().toList()
                    "id_token_encryption_alg_values_supported" -> idTokenEncryptionAlgValuesSupported =
                        readStringArray().toList()

                    "id_token_encryption_enc_values_supported" -> idTokenEncryptionEncValuesSupported =
                        readStringArray().toList()

                    "userinfo_signing_alg_values_supported" -> userinfoSigningAlgValuesSupported =
                        readStringArray().toList()

                    "userinfo_encryption_alg_values_supported" -> userinfoEncryptionAlgValuesSupported =
                        readStringArray().toList()

                    "userinfo_encryption_enc_values_supported" -> userinfoEncryptionEncValuesSupported =
                        readStringArray().toList()

                    "request_object_signing_alg_values_supported" -> requestObjectSigningAlgValuesSupported =
                        readStringArray().toList()

                    "request_object_encryption_alg_values_supported" -> requestObjectEncryptionAlgValuesSupported =
                        readStringArray().toList()

                    "request_object_encryption_enc_values_supported" -> requestObjectEncryptionEncValuesSupported =
                        readStringArray().toList()

                    "backchannel_logout_supported" -> backchannelLogoutSupported = nextBoolean()
                    "backchannel_logout_session_supported" -> backchannelLogoutSessionSupported = nextBoolean()
                    else -> {
                        additional[name] = readJsonValue()
                    }
                }
            }
            endObject()

            OpenIdConfiguration(
                issuer = requireNotNull(issuer) { "issuer is required" },
                authorizationEndpoint = requireNotNull(authorizationEndpoint) { "authorization_endpoint is required" },
                tokenEndpoint = requireNotNull(tokenEndpoint) { "token_endpoint is required" },
                jwksUri = requireNotNull(jwksUri) { "jwks_uri is required" },
                responseTypesSupported = requireNotNull(responseTypesSupported) { "response_types_supported is required" },
                subjectTypesSupported = requireNotNull(subjectTypesSupported) { "subject_types_supported is required" },
                idTokenSigningAlgValuesSupported = requireNotNull(idTokenSigningAlgValuesSupported) { "id_token_signing_alg_values_supported is required" },

                userinfoEndpoint = userinfoEndpoint,
                registrationEndpoint = registrationEndpoint,
                scopesSupported = scopesSupported,
                claimsSupported = claimsSupported,
                grantTypesSupported = grantTypesSupported,
                tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported,
                tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported,
                serviceDocumentation = serviceDocumentation,
                uiLocalesSupported = uiLocalesSupported,

                endSessionEndpoint = endSessionEndpoint,
                checkSessionIframe = checkSessionIframe,

                revocationEndpoint = revocationEndpoint,
                introspectionEndpoint = introspectionEndpoint,
                claimsParameterSupported = claimsParameterSupported,
                requestParameterSupported = requestParameterSupported,
                requestUriParameterSupported = requestUriParameterSupported,
                requireRequestUriRegistration = requireRequestUriRegistration,
                opPolicyUri = opPolicyUri,
                opTosUri = opTosUri,
                codeChallengeMethodsSupported = codeChallengeMethodsSupported,
                idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported,
                idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported,
                userinfoSigningAlgValuesSupported = userinfoSigningAlgValuesSupported,
                userinfoEncryptionAlgValuesSupported = userinfoEncryptionAlgValuesSupported,
                userinfoEncryptionEncValuesSupported = userinfoEncryptionEncValuesSupported,
                requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported,
                requestObjectEncryptionAlgValuesSupported = requestObjectEncryptionAlgValuesSupported,
                requestObjectEncryptionEncValuesSupported = requestObjectEncryptionEncValuesSupported,
                backchannelLogoutSupported = backchannelLogoutSupported,
                backchannelLogoutSessionSupported = backchannelLogoutSessionSupported,

                additional = additional
            )
        }
    }
}
