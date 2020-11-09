package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.junit.jupiter.api.Test

class WildcardTest {

    @Test
    fun `can get out multiple parts`() {
        val a = routes("/{first}" bind routes("/{wildcard:.*}" bind GET to { r: Request ->
            Response(Status.OK).body(r.path("first") + r.path("wildcard"))
        }))

        assertThat(a(Request(GET, "/name/bob/is/great")), hasBody("namebob/is/great"))
    }

    @Test
    fun `can get out path and headers`() {
        val a =
            routes("/name" bind (
                headers("bob") bind routes("/{wildcard:.*}" bind GET to { r: Request ->
                    Response(Status.OK).body(r.path("wildcard").orEmpty())
                })))

        assertThat(a(Request(GET, "/name/bob/is/great")
            .header("bob", "jill")), hasBody("bob/is/great"))
    }

}
