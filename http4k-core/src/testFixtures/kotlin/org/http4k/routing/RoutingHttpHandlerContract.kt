package org.http4k.routing

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asFormatString
import org.http4k.format.Jackson.prettify
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
abstract class RoutingHttpHandlerContract {

    protected open val validPath = "/route-contract"
    protected open val prefix = "/prefix"
    protected open val prePrefix = "/pre-prefix"

    abstract val handler: RoutingHttpHandler

    open val expectedNotFoundBody = ""

    @Test
    fun `matches a particular route`() {
        val criteria = hasStatus(OK)

        assertThat(handler.matchAndInvoke(Request(GET, validPath).header("host", "host")), present(criteria))
        assertThat(handler(Request(GET, validPath).header("host", "host")), criteria)
    }

    @Test
    open fun `does not match a particular route`() {
        assertThat(handler.matchAndInvoke(Request(GET, "/not-found").header("host", "host")), absent())
        assertThat(handler(Request(GET, "/not-found").header("host", "host")), hasStatus(NOT_FOUND) and hasBody(expectedNotFoundBody))
    }

    @Test
    fun `with filter - applies to matching handler`() {
        val filtered = handler.withFilter(filterAppending("bar"))
        val criteria = hasStatus(OK) and hasHeader("res-header", "bar")
        val request = Request(GET, validPath).header("host", "host")

        assertThat(filtered.matchAndInvoke(request), present(criteria))
        assertThat(filtered(request), criteria)
    }

    @Test
    open fun `with filter - applies when not found`() {
        val filtered = handler.withFilter(filterAppending("foo"))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(filtered.matchAndInvoke(request), absent())
        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody(expectedNotFoundBody))
    }

    @Test
    open fun `stacked filter application - applies when not found`() {
        val filtered = filterAppending("foo").then(routes(handler))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(filtered.matchAndInvoke(request), absent())
        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foo") and hasBody(expectedNotFoundBody))
    }

    @Test
    open fun `with filter - applies in correct order`() {
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        val request = Request(GET, "/not-found").header("host", "host")

        assertThat(filtered.matchAndInvoke(request), absent())
        assertThat(filtered(request), hasStatus(NOT_FOUND) and hasHeader("res-header", "foobar"))
    }

    @Test
    fun `with base path - matches`() {
        val withBase = handler.withBasePath(prefix)
        val request = Request(GET, "$prefix$validPath").header("host", "host")
        val criteria = hasStatus(OK)

        assertThat(withBase.matchAndInvoke(request), present(criteria))
        assertThat(withBase(request), criteria)
    }

    @Test
    open fun `with base path - no longer matches original`() {
        val withBase = handler.withBasePath(prefix)
        val request = Request(GET, validPath).header("host", "host")

        assertThat(withBase.matchAndInvoke(request), absent())
        assertThat(withBase(request), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with base path - multiple levels`() {
        val withBasePath = handler.withBasePath(prefix)
        val withBase = withBasePath.withBasePath(prePrefix)
        val request = Request(GET, "$prePrefix$prefix$validPath").header("host", "host")
        val criteria = hasStatus(OK)

        assertThat(withBase.matchAndInvoke(request), present(criteria))
        assertThat(withBase(request), criteria)
    }

    @Test
    fun `can describe routing`(approver: Approver) {
        approver.assertApproved(prettify(asFormatString(handler.description)))
    }

    protected fun filterAppending(value: String) = Filter { next ->
        {
            val response = next(it)
            response.replaceHeader("res-header", response.header("res-header").orEmpty() + value)
        }
    }
}

fun RoutingHttpHandler.matchAndInvoke(request: Request) = when (val matchResult = match(request)) {
    is MatchingHandler -> matchResult(request)
    else -> null
}
