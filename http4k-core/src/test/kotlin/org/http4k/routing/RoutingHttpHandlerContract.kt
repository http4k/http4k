package org.http4k.routing

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.Test

abstract class RoutingHttpHandlerContract {

    protected val validPath = "/route-contract"
    private val prefix = "/prefix"
    private val prePrefix = "/pre-prefix"

    abstract val handler: RoutingHttpHandler

    @Test
    fun `matches a particular route`() {
        assertThat(handler(Request(GET, validPath)), hasStatus(OK))
    }

    @Test
    fun `does not match a particular route`() {
        assertThat(handler(Request(GET, "/not-found")), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with filter - applies to matching handler`() {
        val filter = filterAppending("bar")
        val filtered = handler.withFilter(filter)
        assertThat(filtered(Request(GET, validPath)), hasStatus(OK) and hasHeader("res-header", "bar"))
    }

    @Test
    open fun `with filter - applies when not found`() {
        val filter = filterAppending("foo")
        val filtered = handler.withFilter(filter)
        assertThat(filtered(Request(GET, "/not-found")), hasStatus(NOT_FOUND) and hasHeader("res-header", "foo"))
    }

    @Test
    open fun `with filter - applies in correct order`() {
        val filtered = handler.withFilter(filterAppending("foo")).withFilter(filterAppending("bar"))
        assertThat(filtered(Request(GET, "/not-found")), hasStatus(NOT_FOUND) and hasHeader("res-header", "foobar"))
    }

    @Test
    fun `with base path - matches`() {
        val withBase = handler.withBasePath(prefix)
        assertThat(withBase(Request(GET, "$prefix$validPath")), hasStatus(OK))
    }

    @Test
    fun `with base path - no longer matches original`() {
        val withBase = handler.withBasePath(prefix)
        assertThat(withBase(Request(GET, validPath)), hasStatus(NOT_FOUND))
    }

    @Test
    fun `with base path - multiple levels`() {
        val withBase = handler.withBasePath(prefix).withBasePath(prePrefix)
        assertThat(withBase(Request(GET, "$prePrefix$prefix$validPath")), hasStatus(OK))
    }

    private fun filterAppending(value: String) = Filter { next ->
        { it ->
            val response = next(it)
            response.replaceHeader("res-header", response.header("res-header").orEmpty() + value)
        }
    }
}