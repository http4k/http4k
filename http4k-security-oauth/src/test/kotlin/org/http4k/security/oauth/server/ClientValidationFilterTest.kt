package org.http4k.security.oauth.server

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.junit.jupiter.api.Test

internal class ClientValidationFilterTest {
    private val documentationUri = "SomeUri"
    private val validClientId = ClientId("a-client")
    private val validRedirectUri = Uri.of("https://a-redirect-uri")
    private val validScopes = listOf("openid", "profile")

    private val loginPage = { _: Request -> Response(OK).body("login page") }
    private val isLoginPage = hasStatus(OK) and hasBody("login page")
    private val json = Jackson

    private val authoriseRequestValidator: AuthoriseRequestValidator = object : AuthoriseRequestValidator {

        override fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean = clientId == validClientId

        override fun validate(request: Request, authorizationRequest: AuthRequest): Result<Request, OAuthError> {
            return if (authorizationRequest.client == validClientId) {
                Success(request.header("Success", "true"))
            } else {
                Failure(InvalidClientId)
            }
        }
    }

    private val requestValidator = RequestJWTValidator { _, requestJwtContainer ->
        if (requestJwtContainer.value == "inValidRequest") {
            InvalidAuthorizationRequest("request not correctly signed")
        } else null
    }

    private val authoriseRequestErrorRender = AuthoriseRequestErrorRender(
        authoriseRequestValidator,
        requestValidator,
        JsonResponseErrorRenderer(json, documentationUri),
        documentationUri
    )

    private val filter =
        ClientValidationFilter(authoriseRequestValidator, authoriseRequestErrorRender, AuthRequestFromQueryParameters)
            .then(loginPage)

    @Test
    fun `allow accessing the login page`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", Code.queryParameterValue)
                .query("client_id", validClientId.value)
                .query("redirect_uri", validRedirectUri.toString())
                .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, isLoginPage)
    }

    @Test
    fun `validates presence of client_id`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", Code.queryParameterValue)
                .query("redirect_uri", validRedirectUri.toString())
                .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_request\",\"error_description\":\"query 'client_id' is required\",\"error_uri\":\"SomeUri\"}"))
    }

    @Test
    fun `validates presence of redirect_uri`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", Code.queryParameterValue)
                .query("client_id", validClientId.value)
                .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_request\",\"error_description\":\"query 'redirect_uri' is required\",\"error_uri\":\"SomeUri\"}"))
    }

    @Test
    fun `validates client_id value`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", Code.queryParameterValue)
                .query("client_id", "invalid-client")
                .query("redirect_uri", validRedirectUri.toString())
                .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_client\",\"error_description\":\"The specified client id is invalid\",\"error_uri\":\"SomeUri\"}"))
    }

    @Test
    fun `validates presence of resonse_type`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", "something invalid")
                .query("client_id", validClientId.value)
                .query("redirect_uri", validRedirectUri.toString())
                .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, equalTo(Response(SEE_OTHER).header("Location", "https://a-redirect-uri?error=unsupported_response_type&error_description=The+specified+response_type+%27something+invalid%27+is+not+supported&error_uri=SomeUri")))
    }

    @Test
    fun `validates presence of resonse_type, even taking into account response mode, and with state`() {
        val response = filter(
            Request(GET, "/auth")
                .query("response_type", "something invalid")
                .query("response_mode", "fragment")
                .query("client_id", validClientId.value)
                .query("redirect_uri", validRedirectUri.toString())
                .query("state", "someState")
        )
        assertThat(response, equalTo(Response(SEE_OTHER).header("Location", "https://a-redirect-uri#state=someState&error=unsupported_response_type&error_description=The+specified+response_type+%27something+invalid%27+is+not+supported&error_uri=SomeUri")))
    }
}
