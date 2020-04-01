package org.http4k.testing

import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.FormField
import org.http4k.lens.Header.LOCATION
import org.http4k.lens.Query
import org.http4k.lens.Validator.Feedback
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.OAuthProviderConfig
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.renderToResponse
import java.util.UUID
import java.util.UUID.randomUUID


class FakeOAuthServer(oAuthClientConfig: OAuthProviderConfig, serviceName: String) : HttpHandler {

    private val templates = HandlebarsTemplates().CachingClasspath()

    private val user = FormField.required("user")
    private val password = FormField.required("password")
    private val callbackUri = FormField.uri().required("callbackUri")
    private val loginForm = Body.webForm(Feedback, user, password, callbackUri).toLens()
    private val html = Body.string(TEXT_HTML).toLens()

    private val redirectUri = Query.uri().required("redirect_uri")
    private val users = mapOf("user" to "password")

    private val generatedCodes = mutableMapOf<UUID, Credentials>()

    private val login: HttpHandler = { templates.renderToResponse(OAuthLogin(serviceName, redirectUri(it))) }

    private val submit: HttpHandler = {
        val submitted = loginForm(it)
        val credentials = Credentials(user(submitted), password(submitted))
        when {
            submitted.errors.isNotEmpty() || users[credentials.user] != credentials.password ->
                Response(OK).with(html of templates(OAuthLogin("Google", callbackUri(submitted), "failed")))
            else -> randomUUID().let {
                generatedCodes[it] = credentials
                Response(TEMPORARY_REDIRECT).with(LOCATION of callbackUri(submitted).query("code", it.toString()))
            }
        }
    }

    /*
    "{ \"access_token\": \"ya29.GluJBWUSr_cvzC2J4hl0kxD8SmyUlspVwkNrf9oKPGEOFgwNxU_JipNPO0NP76MCUsnsvQ9G-YbH3mni6wcPwXoqSpdZdUa5QQQvKtJZNNj2z7WMhw5hX3_RSeZA\", \"token_type\": \"Bearer\", \"expires_in\": 3600, \"id_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjM3NmVhMWUyZjRjOTM3YzMzM2QxZTI0YjU2NDczOGZjMDRjOTkwMDkifQ.eyJhenAiOiI5MTkwOTMzNzc5NzEtN2M3djBxdDhramZhdm4xcml0M3U1bjluajZ0ajY2b2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiI5MTkwOTMzNzc5NzEtN2M3djBxdDhramZhdm4xcml0M3U1bjluajZ0ajY2b2QuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDU5MTQ0NDczNzAwNTg0NzUzNTUiLCJlbWFpbCI6ImRlbnRvbi5kYXZpZEBnbWFpbC5jb20iLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXRfaGFzaCI6IkFQazBJUGVRS3docU5vMUw3VkFDU0EiLCJub25jZSI6ImhrazFtb2tvaGxvdmxib3Vpdmg4N2JocTMxIiwiZXhwIjoxNTIxOTc5Njk4LCJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJpYXQiOjE1MjE5NzYwOTgsIm5hbWUiOiJEYXZpZCBEZW50b24iLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDUuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy0zWUpOSWJBSDFpTS9BQUFBQUFBQUFBSS9BQUFBQUFBQUI0Zy9YQlhHNXk3dlRiZy9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiRGF2aWQiLCJmYW1pbHlfbmFtZSI6IkRlbnRvbiIsImxvY2FsZSI6ImVuLUdCIn0.K8r3cFCYm42mZSAeWy5-TkWu5cMKf3V_To_bNVEQZO8q5ymD4Sal0cgen2O68yK2BJgmBfNxej6IKaAuvQ4czRaNQxcjZaZS4HxIa-S1O7Y4TISkuNUxsAt94mfaQzosMqGihCmw2vTUmSo2LyZA9D47EmYrz7fjcP8CwbGzUnm_o3VXgfA_Qel89WpRFfc8BJl8qTgexYHm4VOCvT5QM0FdVGXhlpdlNrZCrppmhZJPiApODLfx5XhvO7-k0oBlv89Tl14GLAfbxKEl6oNeOQjz_JBAVcTPHMzvQk_7nuL5zvbdJsCVSp25xl_q37ALSbFstOPzuNHY9NGEewTW9w\"
     */
    private val generateAccessToken: (Request) -> Response = {


        Response(FORBIDDEN)
    }

    private val api = ServerFilters.CatchAll().then(
        routes(
            oAuthClientConfig.authPath bind POST to login,
            oAuthClientConfig.tokenPath bind POST to generateAccessToken,
            "/fakeLogin" bind POST to submit,
            "/" bind GET to { Response(OK).with(html of templates(OAuthIndex(serviceName))) }
        )
    )

    override fun invoke(p1: Request): Response = api(p1)
}
