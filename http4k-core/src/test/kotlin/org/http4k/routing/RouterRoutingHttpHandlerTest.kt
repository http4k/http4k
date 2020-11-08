package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.UriTemplate
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class RouterRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = RouterRoutingHttpHandler(TemplateRouter(
        template = UriTemplate.from(validPath),
        httpHandler = { Response(OK) }
    )

        @Test
        fun `multi param routes`() {
            val handler = routes("/{foo}" bind Method.GET to routes("/{bar}" bind { it: Request ->
                Response(OK).body(it.path("foo")!! + " then " + it.path("bar"))
            }))

            assertThat(handler(Request(Method.GET, "/one/two")), hasStatus(OK).and(hasBody("one then two")))
        }

}
