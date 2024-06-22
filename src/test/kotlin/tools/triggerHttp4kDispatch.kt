import org.http4k.client.JavaHttpClient
import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.ClientFilters.SetHostFrom

const val version = ""

fun main() {

    if (version.isNotEmpty()) {
        val event = mapOf("event_type" to "http4k-release", "client_payload" to mapOf("version" to version))

        val client = SetHostFrom(Uri.of("https://api.github.com"))
            .then(BearerAuth(System.getenv("GITHUB_TOKEN")))
            .then(JavaHttpClient())

        val repo = "http4k/http4k-connect"
        val request = Request(POST, Uri.of("/repos/$repo/dispatches"))
            .with(Body.auto<Map<String, Any>>().toLens() of event)

        println(client(request))
    }
}
