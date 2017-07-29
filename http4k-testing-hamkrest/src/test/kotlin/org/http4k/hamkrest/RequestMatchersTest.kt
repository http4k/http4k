package org.http4k.hamkrest

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.junit.Test

class RequestMatchersTest {

    @Test
    fun `method`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasMethod(GET), hasMethod(POST))

    @Test
    fun `uri as string`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasUri("/bob"), hasUri("/bill"))

    @Test
    fun `uri as uri`() = assertMatchAndNonMatch(Request(GET, "/bob"), hasUri(Uri.of("/bob")), hasUri(Uri.of("/bill")))

    @Test
    fun `query`() = assertMatchAndNonMatch(Request(GET, "/bob?query=bob"), hasQuery("query", "bob"), hasQuery("query", "bill"))

    @Test
    fun `queries`() = assertMatchAndNonMatch(Request(GET, "/bob?query=bob&query=bob2"), hasQuery("query", listOf("bob", "bob2")), hasQuery("query", listOf("bill")))
}