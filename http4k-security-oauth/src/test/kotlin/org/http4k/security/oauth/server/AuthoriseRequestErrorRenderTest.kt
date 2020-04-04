package org.http4k.security.oauth.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.security.ResponseMode.Query
import org.http4k.security.ResponseType.Code
import org.http4k.security.ResponseType.CodeIdToken
import org.http4k.security.oauth.server.request.RequestJWTValidator
import org.http4k.security.openid.RequestJwtContainer
import org.junit.jupiter.api.Test
import java.util.Base64

internal class AuthoriseRequestErrorRenderTest {

    @Test
    fun `when request doesn't have client_id then render json`() {
        assertThat(underTest.errorFor(Request(GET, "/"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `when request doesn't have redirect_uri then render json`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `when request doesn't have valid redirect_uri then render json`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someInvalidHost"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `If has both valid client id and redirect_uri then redirect to redirect_uri`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If has both valid client id and redirect_uri then redirect to redirect_uri, also include state`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&state=someValidState"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?state=someValidState&error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If has both valid client id and redirect_uri then redirect to redirect_uri, also include state, taking into account response_mode`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&state=someValidState&response_mode=fragment"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost#state=someValidState&error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If request is included but is invalid then don't redirect even if all else is valid`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&state=someState&request=inValidRequest"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `If state is in request jwt not request uri then use the request one`() {
        val state = "someValidState"
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(state = state)}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?state=$state&error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If same state is in request jwt and uri include it in response`() {
        val state = "someValidState"
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(state = state)}&state=$state"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?state=$state&error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If different state is in request jwt and uri don't include it in response`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(state = "someDifferentState")}&state=someValidState"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If response type is in request jwt not request uri then use the request one`() {
        val responseType = CodeIdToken.queryParameterValue
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = responseType)}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost#error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If same response type is in request jwt and request uri then include it in the request`() {
        val responseType = CodeIdToken.queryParameterValue
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = responseType)}&response_type=$responseType"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost#error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If different response type is in request jwt then in the request uri then exclude it in the request`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = Code.queryParameterValue)}&response_type=${CodeIdToken.queryParameterValue}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }


    @Test
    fun `If response mode is in request jwt not request uri then use the request one`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = CodeIdToken.queryParameterValue, responseMode = Query.queryParameterValue)}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If same response mode is in request jwt and request uri then include it in the request`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = CodeIdToken.queryParameterValue, responseMode = Query.queryParameterValue)}&response_mode=query"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If different response mode is in request jwt then in the request uri then exclude it in the request`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = CodeIdToken.queryParameterValue, responseMode = Query.queryParameterValue)}&response_mode=fragment"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost#error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If response_mode is invalid in jwt then treat it as null`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = CodeIdToken.queryParameterValue, responseMode = "someThingInvalid")}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost#error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If response_type is invalid in jwt then treat it as null`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(responseType = "somethingInvalid")}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If response_mode is invalid in request then treat it as null`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&response_mode=sdkfjsklfjskldfj"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If response_type is invalid in request then treat it as null`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&response_type=sdkfjsklfjskldfj"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }


    @Test
    fun `If redirect_uri is in request jwt not request uri then use the request one`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&request=${generateARequestJwt(redirectUri = "https://someValidHost")}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If same redirect_uri is in request jwt and request uri then include it in the request`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(redirectUri = "https://someValidHost")}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If different redirect_uri is in request jwt then in the request uri then exclude it in the request`() {
        assertThat(underTest.errorFor(Request(GET, "/?client_id=validClient&redirect_uri=https://someValidHost&request=${generateARequestJwt(redirectUri = "https://somewhereElse")}"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `If same client_id is in request jwt and request uri then include it in the request`() {
        val clientId = "validClient"
        assertThat(underTest.errorFor(Request(GET, "/?client_id=$clientId&redirect_uri=https://someValidHost&request=${generateARequestJwt(clientId = clientId)}"), ClientIdMismatch), equalTo(Response(SEE_OTHER)
            .header("Location", "https://someValidHost?error=invalid_grant&error_description=The+%27client_id%27+parameter+does+not+match+the+authorization+request&error_uri=https%3A%2F%2FsomeDocumentationUri")))
    }

    @Test
    fun `If different client_id is in request jwt and request uri then treat it as no client id`() {
        val clientId = "validClient"
        assertThat(underTest.errorFor(Request(GET, "/?client_id=$clientId&redirect_uri=https://someValidHost&request=${generateARequestJwt(clientId = "someOtherValidClientId")}"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }

    @Test
    fun `If client_id is in request jwt and not in request uri then treat it as no client id`() {
        assertThat(underTest.errorFor(Request(GET, "/?redirect_uri=https://someValidHost&request=${generateARequestJwt(clientId = "validClient")}"), ClientIdMismatch), equalTo(Response(BAD_REQUEST)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body("{\"error\":\"invalid_grant\",\"error_description\":\"The 'client_id' parameter does not match the authorization request\",\"error_uri\":\"https://someDocumentationUri\"}")))
    }


    private val authoriseRequestValidator = object : AuthoriseRequestValidator {

        private val validClients = mapOf(
            ClientId("validClient") to Uri.of("https://someValidHost")
        )

        override fun isValidClientAndRedirectUriInCaseOfError(request: Request, clientId: ClientId, redirectUri: Uri): Boolean =
            validClients[clientId] == redirectUri

        override fun validate(request: Request, authorizationRequest: AuthRequest) =
            throw UnsupportedOperationException("not needed for this test")

    }

    private val requestValidator = object : RequestJWTValidator {
        override fun validate(clientId: ClientId, requestJwtContainer: RequestJwtContainer): InvalidAuthorizationRequest? {
            return if (requestJwtContainer.value == "inValidRequest") {
                InvalidAuthorizationRequest("request not correctly signed")
            } else null
        }

    }

    private val documentationUri = "https://someDocumentationUri"

    private val underTest = AuthoriseRequestErrorRender(
        authoriseRequestValidator,
        requestValidator,
        JsonResponseErrorRenderer(Jackson, documentationUri),
        documentationUri)

    private fun generateARequestJwt(state: String? = null,
                                    responseType: String? = null,
                                    responseMode: String? = null,
                                    clientId: String? = null,
                                    redirectUri: String? = null): String {
        val data = mutableMapOf<String, Any>()
        if (state != null) {
            data["state"] = state
        }
        if (responseType != null) {
            data["response_type"] = responseType
        }
        if (responseMode != null) {
            data["response_mode"] = responseMode
        }
        if (redirectUri != null) {
            data["redirect_uri"] = redirectUri
        }
        if (clientId != null) {
            data["client_id"] = clientId
        }
        return "someSuperValidHeader.${Base64.getUrlEncoder().encodeToString(Jackson.asJsonString(data).toByteArray()).replace("=", "")}.someSuperValidSignature"
    }
}
