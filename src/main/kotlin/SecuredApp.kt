
import org.http4k.client.ApacheClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header.Common.LOCATION
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel


val client = ApacheClient()

val credentials = Credentials("c090918276dd3545e779f29f61c86c7a", "ca78037a2fe013aa120b3de628375b4f")

//fun main(args: Array<String>) {
//
//    val auth = AuthorizeRequest(credentials, Uri.of("http://localhost:9000"))
//
//    val r = client(Request(GET, "https://soundcloud.com/connect").with(auth))
//
//    println(r)
//}
//

data class Index(val name: String) : ViewModel

/**
Create an anti-forgery state token
Send an authentication request to Google
https://accounts.google.com/o/oauth2/v2/auth?
client_id=424911365001.apps.googleusercontent.com&
response_type=code&
scope=openid%20email&
redirect_uri=http://localhost:9000/callback
state=below
encode!!    security_token=138r5719ru3e1&url=https://oauth2-login-demo.example.com/myHome&
nonce=random&

Confirm the anti-forgery state token = check value against cookie

Exchange code for access token and ID token
POST www.googleapis.com/oauth2/v4/token
Content-Type: application/x-www-form-urlencoded
code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7&
client_id=8819981768.apps.googleusercontent.com&
client_secret={client_secret}&
redirect_uri=https://oauth2-login-demo.example.com/code&
grant_type=authorization_code

returns JSON with:
access_token	A token that can be sent to a Google API.
id_token	A JWT that contains identity information about the user that is digitally signed by Google.
expires_in	The remaining lifetime of the access token.
token_type	Identifies the type of token returned. At this time, this field always has the value Bearer.
refresh_token (optional)

Obtain user information from the ID token

Authenticate the user
 */

fun main(args: Array<String>) {
    val templates = HandlebarsTemplates().CachingClasspath()

    val home = Uri.of(("http://localhost:9000"))

    val soundcloudHome = Uri.of("https://soundcloud.com")

    //https://www.googleapis.com/auth/calendar

    fun getIndex(code: String) = Response(OK).body(templates(Index(code)))

    val index = { r: Request ->
        r.cookie("full")?.let { getIndex(it.value) }
            ?: r.query("code")?.let { Response(TEMPORARY_REDIRECT).cookie(Cookie("session", it)).with(LOCATION of home) }
//            ?: Response(TEMPORARY_REDIRECT).with(LOCATION of soundcloudHome.with(GoogleAuthorizeRequest(credentials, home)))
    }

    val app: HttpHandler = routes("/" bind GET to { Response(OK) })

    ServerFilters.CatchAll().then(app).asServer(SunHttp(9000)).start().block()
}
