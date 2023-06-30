package org.http4k.contract.openapi.v3

import com.fasterxml.jackson.annotation.JsonProperty
import org.http4k.contract.contract
import org.http4k.contract.meta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenAPIJackson
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test

class JacksonAnnotatedFieldsOpenApiTest {

    data class Obj(@JsonProperty("Name") val name: String)

    val app = contract {
        renderer = OpenApi3(ApiInfo("", ""))
        routes += "/echo" meta {
            receiving(OpenAPIJackson.autoBody<Obj>().toLens() to Obj("jim"))
        } bindContract Method.POST to { _: Request -> Response(Status.OK) }
    }

    @Test
    fun `does not blow up when rendering open api`() {
        app(Request(GET, "/"))
    }
}
