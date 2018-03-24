package fake

import GoogleApi
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.FormField
import org.http4k.lens.Header.Common.LOCATION
import org.http4k.lens.Validator.Feedback
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.HandlebarsTemplates
import java.util.UUID
import java.util.UUID.randomUUID

class FakeGoogleApi : HttpHandler {

    private val templates = HandlebarsTemplates().CachingClasspath()

    private val user = FormField.required("user")
    private val password = FormField.required("password")
    private val callbackUri = FormField.uri().required("callbackUri")
    private val loginForm = Body.webForm(Feedback, user, password, callbackUri).toLens()
    private val html = Body.string(TEXT_HTML).toLens()

    private val users = mapOf("user" to "password")
    private val codes = mutableMapOf<UUID, Credentials>()

    private val login: HttpHandler = {
        Response(Status.OK).with(html of templates(OAuthLogin("Google", GoogleApi.redirectUri(it))))
    }

    private val submit: HttpHandler = {
        val submitted = loginForm(it)
        val credentials = Credentials(user(submitted), password(submitted))
        when {
            submitted.errors.isNotEmpty() || users[credentials.user] != credentials.password ->
                Response(OK).with(html of templates(OAuthLogin("Google", callbackUri(submitted), "failed")))
            else -> randomUUID().let {
                codes[it] = credentials
                Response(TEMPORARY_REDIRECT).with(LOCATION of callbackUri(submitted).query("code", it.toString()))
            }
        }
    }

    private val api = ServerFilters.CatchAll().then(
        routes(
            "/o/oauth2/v2/auth" bind POST to login,
            "/fakeLogin" bind POST to submit,
            "/" bind GET to { Response(OK).with(html of templates(Index("Google"))) }
        )
    )

    override fun invoke(p1: Request): Response = api(p1)
}
