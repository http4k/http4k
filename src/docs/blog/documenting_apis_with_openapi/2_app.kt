package blog.documenting_apis_with_openapi

import org.http4k.contract.contract
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v3.OpenApi3
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.format.Jackson
import org.http4k.server.Undertow
import org.http4k.server.asServer

fun main() {
    val http: HttpHandler = contract {
        renderer = OpenApi3(
            ApiInfo("my secure api", "v1.0", "API description"),
            json = Jackson
        )
        descriptionPath = "/reference/api/swagger.json"
        security = BasicAuthSecurity("realm", Credentials("user", "password"))
        routes += basicRoute
    }

    http.asServer(Undertow(9000)).start()
}
