package org.http4k.routing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.experimental.RoutedHttpHandler
import org.http4k.routing.experimental.newBind
import org.http4k.routing.experimental.newRoutes
import org.junit.jupiter.api.Test

fun newSinglePageApp(
    resourceLoader: ResourceLoader = ResourceLoader.Classpath("/public"),
    vararg extraFileExtensionToContentTypes: Pair<String, ContentType>
): RoutedHttpHandler =
    RoutedHttpHandler(
        listOf(
            SinglePageAppRouteMatcher(
                "",
                NewStaticRouteMatcher("", resourceLoader, extraFileExtensionToContentTypes.asList().toMap())
            )
        )
    )

class NewSinglePageAppRoutingHttpHandlerTest {
    private val validPath = "/route-contract"
    private val prefix = "/prefix"

    val handler = newRoutes(
        validPath newBind newSinglePageApp(
            ResourceLoader.Classpath()
        )
    )

    @Test
    fun `with filter - applies in correct order`() {
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        val request = Request(GET, "/not-found")
        val criteria = isHomePage() and hasHeader("res-header", "foobar")

        assertThat(filtered(request), criteria)
    }

    @Test
    fun `stacked filter application - applies when not found`() {
        val filtered = filterAppending("foo").then(newRoutes(handler))
        val request = Request(GET, "/not-found")
        val criteria = isHomePage() and hasHeader("res-header", "foo")

        assertThat(filtered(request), criteria)
    }

    @Test
    fun `with filter - applies when not found`() {
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found")
        val criteria = isHomePage() and hasHeader("res-header", "foo")

        assertThat(filtered(request), criteria)
    }

    @Test
    fun `does not match a particular route`() {
        val request = Request(GET, "/not-found")
        val criteria = isHomePage()

        assertThat(handler(request), criteria)
    }

    @Test
    fun `with base path - no longer matches original`() {
        val criteria = isHomePage()
        val request = Request(GET, validPath)
        val withBasePath = handler.withBasePath(prefix)

        assertThat(withBasePath(request), criteria)
    }

    @Test
    fun `does not match non-GET requests for valid path`() {
        assertThat(handler(Request(OPTIONS, validPath)), hasStatus(OK))
        assertThat(handler(Request(GET, validPath)), hasStatus(OK))
    }

    @Test
    fun `does not interfere with CORs policy`() {
        val app = Cors(UnsafeGlobalPermissive)
            .then(
                newRoutes(
                    "/api/{name}" newBind  GET to { Response(OK).body(it.path("name")!!) },
                    newSinglePageApp()
                )
            )

        val optionsResponse = hasStatus(OK).and(hasBody(""))

        assertThat(
            app(Request(OPTIONS, "/api/ken").header("Origin", "foo")),
            optionsResponse
        )

        assertThat(
            app(Request(GET, "/api/ken").header("Origin", "foo")),
            hasStatus(OK).and(hasBody("ken"))
        )

        assertThat(
            app(Request(GET, "/index").header("Origin", "foo")),
            isHomePage("public")
        )

        assertThat(
            app(Request(OPTIONS, "/index").header("Origin", "foo")),
            optionsResponse
        )
    }

    @Test
    fun `DSL construction defaults to using public as a root path`() {
        val dslDefault = newSinglePageApp()
        val criteria = isHomePage("public")

        println(dslDefault(Request(GET, validPath)))
        assertThat(dslDefault(Request(GET, validPath)), criteria)
    }

    private fun isHomePage(name: String = "root"): Matcher<Response> = hasStatus(OK)
        .and(hasBody("hello from the $name index.html"))
        .and(hasHeader("Content-Type", equalTo(ContentType.TEXT_HTML.value)))

    private fun filterAppending(value: String) = Filter { next ->
        {
            val response = next(it)
            response.replaceHeader("res-header", response.header("res-header").orEmpty() + value)
        }
    }
}
