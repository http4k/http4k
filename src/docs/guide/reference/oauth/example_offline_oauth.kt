package guide.reference.oauth

import org.http4k.client.JavaHttpClient
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.client.oAuthOffline

fun main() {
    // set these before running example
    val refreshToken = System.getenv("REFRESH_TOKEN")
    val clientCredentials = Credentials(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET"))
    val authServerBase = Uri.of(System.getenv("OAUTH_AUTH_SERVER_HOST"))
    val resourceServerHost = Uri.of(System.getenv("OAUTH_RESOURCE_SERVER_HOST"))

    // all the configuration we need to talk to our OAuth Auth Server (Google, Auth0, Okta, etc.)
    val config = OAuthProviderConfig(
        authBase = authServerBase,
        authPath = "/oauth2/authorize",
        tokenPath = "/oauth2/token",
        credentials = clientCredentials
    )

    // construct a client with a filter to authorize our requests to the OAuth Resource server
    val client = ClientFilters.SetHostFrom(resourceServerHost)
        .then(ClientFilters.oAuthOffline(config, refreshToken, JavaHttpClient()))
        .then(JavaHttpClient())

    // Make a request to the OAuth Resource server
    val request = Request(GET, "/v1/secure/resource")
    val response = client(request)
    println(response)
}
