package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Result
import com.natpryce.Success
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.security.ResponseType.Code
import org.junit.jupiter.api.Test

internal class ClientValidationFilterTest {
    private val documentationUri = "SomeUri"
    private val validClientId = ClientId("a-client")
    private val validRedirectUri = Uri.of("https://a-redirect-uri")
    private val validScopes = listOf("openid", "profile")

    private val loginPage = { _: Request -> Response(OK).body("login page") }
    private val isLoginPage = hasStatus(OK) and hasBody("login page")
    private val json = Jackson

    private val  authoriseRequestValidator: AuthoriseRequestValidator = object : AuthoriseRequestValidator{
        override fun validate(request: Request, authorizationRequest: AuthRequest): Result<AuthRequest, OAuthError> {
            if(authorizationRequest.client == validClientId) {
                return Success(authorizationRequest)
            } else {
                return Failure(InvalidClientId)
            }
        }

    }

    private val filter =
        ClientValidationFilter(authoriseRequestValidator, ErrorRenderer(json, documentationUri), AuthRequestFromQueryParameters)
            .then(loginPage)


    @Test
    fun `allow accessing the login page`() {
        val response = filter(Request(GET, "/auth")
            .query("response_type", Code.queryParameterValue)
            .query("client_id", validClientId.value)
            .query("redirect_uri", validRedirectUri.toString())
            .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, isLoginPage)
    }

    @Test
    fun `validates presence of client_id`() {
        val response = filter(Request(GET, "/auth")
            .query("response_type", Code.queryParameterValue)
            .query("redirect_uri", validRedirectUri.toString())
            .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_request\",\"error_description\":\"query 'client_id' is required\",\"error_uri\":\"SomeUri\"}"))
    }

    @Test
    fun `validates presence of redirect_uri`() {
        val response = filter(Request(GET, "/auth")
            .query("response_type", Code.queryParameterValue)
            .query("client_id", validClientId.value)
            .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_request\",\"error_description\":\"query 'redirect_uri' is required\",\"error_uri\":\"SomeUri\"}"))
    }

    @Test
    fun `validates client_id value`() {
        val response = filter(Request(GET, "/auth")
            .query("response_type", Code.queryParameterValue)
            .query("client_id", "invalid-client")
            .query("redirect_uri", validRedirectUri.toString())
            .query("scope", validScopes.joinToString(" "))
        )
        assertThat(response, hasStatus(BAD_REQUEST))
        assertThat(response.bodyString(), equalTo("{\"error\":\"invalid_client\",\"error_description\":\"The specified client id is invalid\",\"error_uri\":\"SomeUri\"}"))
    }

}

