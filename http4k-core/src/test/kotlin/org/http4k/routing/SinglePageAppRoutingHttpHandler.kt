package org.http4k.routing

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
        assertThat(filtered(Request(GET, "/not-found")), isHomePage() and hasHeader("res-header", "foobar"))
    }

    @Test
    override fun `with filter - applies when not found`() {
        val filtered = handler.withFilter(filterAppending("foo"))
        assertThat(filtered(Request(GET, "/not-found")), isHomePage() and hasHeader("res-header", "foo"))
    }

    @Test
    override fun `does not match a particular route`() {
        assertThat(handler(Request(GET, "/not-found")), isHomePage())
    }

    @Test
    override fun `with base path - no longer matches original`() {
        assertThat(handler.withBasePath(prefix)(Request(GET, validPath)), isHomePage())
    }

    private fun isHomePage(): Matcher<Response> = hasStatus(Status.OK)
        .and(hasBody("hello from the root index.html"))
        .and(hasHeader("Content-Type", equalTo(ContentType.TEXT_HTML.value)))

}