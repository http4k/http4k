package org.http4k.routing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class SinglePageAppRoutingHttpHandlerTest  : RoutingHttpHandlerContract() {
    override val handler = validPath bind singlePageApp(
        ResourceLoader.Classpath()
    )

    @Test
    override fun `with filter - applies in correct order`() {
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        val request = Request(GET, validPath)
        val criteria = isHomePage() and hasHeader("res-header", "foobar")

        assertThat(filtered(request), criteria)
    }

    @Test
    override fun `stacked filter application - applies when not found`() {
        val filtered = filterAppending("foo").then(routes(handler))
        val request = Request(GET, "/not-found")
        val criteria = hasHeader("res-header", "foo")

        assertThat(filtered(request), criteria)
    }

    @Test
    override fun `with filter - applies when not found`() {
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found")
        val criteria = hasHeader("res-header", "foo")

        assertThat(filtered(request), criteria)
    }

    @Test
    override fun `does not match a particular route`() {
        val request = Request(GET, "/not-found")
        assertThat(handler(request), hasStatus(NOT_FOUND))
    }

    @Test
    override fun `with base path - no longer matches original`() {
        val criteria = isHomePage()
        val withBasePath = handler.withBasePath(prefix)

        assertThat(withBasePath(Request(GET, validPath)), hasStatus(NOT_FOUND))
        assertThat(withBasePath(Request(GET, prefix + validPath)), criteria)

    }

    @Test
    fun `does not match non-GET requests for valid path`() {
        assertThat(handler(Request(OPTIONS, validPath)), hasStatus(NOT_FOUND))
        assertThat(handler(Request(GET, validPath)), hasStatus(OK))
    }

    @Test
    fun `does not interfere with CORs policy`() {
        val app = Cors(UnsafeGlobalPermissive)
            .then(
                routes(
                    "/api/{name}" bind GET to { Response(OK).body(it.path("name")!!) },
                    singlePageApp()
                )
            )

        val optionsResponse = hasStatus(OK).and(hasBody(""))

        assertThat(
            app(Request(GET, "/").header("Origin", "foo")),
            isHomePage("public")
        )

        assertThat(
            app(Request(OPTIONS, "/api/ken").header("Origin", "foo")),
            optionsResponse
        )

        assertThat(
            app(Request(GET, "/api/ken").header("Origin", "foo")),
            hasStatus(OK).and(hasBody("ken"))
        )

        assertThat(
            app(Request(OPTIONS, "/index").header("Origin", "foo")),
            optionsResponse
        )
    }

    @Test
    fun `DSL construction defaults to using public as a root path`() {
        val dslDefault = singlePageApp()
        val criteria = isHomePage("public")

        assertThat(dslDefault(Request(GET, "/")), criteria)
    }

    private fun isHomePage(name: String = "root"): Matcher<Response> = hasStatus(OK)
        .and(hasBody("hello from the $name index.html"))
        .and(hasHeader("Content-Type", equalTo(ContentType.TEXT_HTML.value)))
}
