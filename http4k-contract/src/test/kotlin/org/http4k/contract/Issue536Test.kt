package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

class Issue536Test {

    @Test
    fun `path parameters are decoded correctly`() {
        val app = routes(
            "/" bind contract {
                routes += Path.of("name") bindContract GET to { name ->
                    assertThat(name, equalTo("Günter"))
                    ({ Response(OK) })
                }
            }
        )

        assertThat(app(Request(GET, "/G%C3%BCnter")), hasStatus(OK))
    }
}

