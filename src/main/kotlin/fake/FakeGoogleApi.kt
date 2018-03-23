package fake

import GoogleApi
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.FormField
import org.http4k.lens.Header
import org.http4k.lens.Validator
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import java.util.UUID

class FakeGoogleApi : HttpHandler {

    private val templates = HandlebarsTemplates().CachingClasspath()

    private val user = FormField.required("user")
    private val password = FormField.required("password")
    private val callbackUri = FormField.uri().required("callbackUri")
    private val loginForm = Body.webForm(Validator.Feedback, user, password, callbackUri).toLens()
    private val html = Body.string(ContentType.TEXT_HTML).toLens()

    private val users = mapOf("user" to "password")
    private val codes = mutableMapOf<UUID, Credentials>()

    private val login: HttpHandler = {
        Response(Status.OK).with(html of templates(OAuthLogin("Google", GoogleApi.redirectUri(it))))
    }

    private val submit: HttpHandler = {
        val submitted = loginForm(it)
        val credentials = Credentials(user(submitted), password(submitted))
        if (users[credentials.user] == credentials.password) {
            UUID.randomUUID().let {
                codes[it] = credentials
                Response(Status.TEMPORARY_REDIRECT).with(Header.Common.LOCATION of callbackUri(submitted).query("code", it.toString()))
            }
        } else {
            Response(OK).with(html of templates(OAuthLogin("Google", callbackUri(submitted), "failed")))
        }
    }

    private val api =
        ServerFilters.CatchAll().then(
            routes(
                "/o/oauth2/v2/auth" bind Method.POST to login,
                "/fakeLogin" bind Method.POST to submit,
                "/" bind Method.GET to { Response(OK).with(html of templates(Index("Google"))) }
            )
        )

    override fun invoke(p1: Request): Response = api(p1)
}

fun main(args: Array<String>) {
    FakeGoogleApi().asServer(SunHttp(8000)).start().block()
}