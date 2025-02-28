package org.http4k.security.oauth.metadata

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.security.ResponseType
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Locale

@ExtendWith(JsonApprovalTest::class)
class WellKnownMetadataTest {

    @Test
    fun `gets metadata`(approver: Approver) {
        val metadata = ServerMetadata(
            issuer = "https://example.com",
            authorization_endpoint = Uri.of("https://example.com/auth"),
            token_endpoint = Uri.of("https://example.com/token"),
            token_endpoint_auth_methods_supported = listOf(AuthMethod.client_secret_basic),
            token_endpoint_auth_signing_alg_values_supported = listOf("RS256"),
            response_types_supported = listOf(ResponseType.Code),
            ui_locales_supported = listOf(Locale.forLanguageTag("en-GB"))
        )

        val http = WellKnownMetadata(metadata)

        approver.assertApproved(http(Request(GET, "/.well-known/oauth-authorization-server")))
    }
}
