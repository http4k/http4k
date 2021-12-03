package guide.reference.digest

import org.http4k.client.JavaHttpClient
import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DigestAuth

fun main() {
    val credentials = Credentials("admin", "password")

    val client = ClientFilters.DigestAuth(credentials)
        .then(JavaHttpClient())

    val request = Request(GET, "http://localhost:8000/hello/http4k")

    val response = client(request)
    println(response)
}
