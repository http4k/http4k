package org.http4k.routing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.ContentType
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

class SinglePageAppRoutingHttpHandlerTest : RoutingHttpHandlerContract() {
    override val handler: RoutingHttpHandler = SinglePageAppHandler(validPath,
        StaticRoutingHttpHandler(
            pathSegments = validPath,
            resourceLoader = ResourceLoader.Classpath(),
            extraFileExtensionToContentTypes = emptyMap()
        )
    )

    @Test
    override fun `with filter - applies in correct order`() {
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        val request = Request(GET, "/not-found")
        val criteria = isHomePage() and hasHeader("res-header", "foobar")

        assertThat(handler.matchAndInvoke(request), present(criteria))
        assertThat(filtered(request), criteria)
    }

    @Test
    override fun `with filter - applies when not found`() {
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found")
        val criteria = isHomePage() and hasHeader("res-header", "foo")

        assertThat(handler.matchAndInvoke(request), present(criteria))
        assertThat(filtered(request), criteria)
    }

    @Test
    override fun `does not match a particular route`() {
        val request = Request(GET, "/not-found")
        val criteria = isHomePage()

        assertThat(handler.matchAndInvoke(request), present(criteria))
        assertThat(handler(request), criteria)
    }

    @Test
    override fun `with base path - no longer matches original`() {
        val criteria = isHomePage()
        val request = Request(GET, validPath)
        val withBasePath = handler.withBasePath(prefix)

        assertThat(handler.matchAndInvoke(request), present(criteria))
        assertThat(withBasePath(request), criteria)
    }

    private fun isHomePage(): Matcher<Response> = hasStatus(Status.OK)
        .and(hasBody("hello from the root index.html"))
        .and(hasHeader("Content-Type", equalTo(ContentType.TEXT_HTML.value)))

}