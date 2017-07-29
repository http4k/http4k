package org.http4k.hamkrest

import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.Test

class HttpMessageMatchersTest {

    @Test
    fun `header`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob"), hasHeader("header", "bob"), hasHeader("header", "bill"))

    @Test
    fun `headers`() = assertMatchAndNonMatch(Request(GET, "/").header("header", "bob").header("header", "bob2"), hasHeader("header", listOf("bob", "bob2")), hasHeader("header", listOf("bill")))

    @Test
    fun `content type`() = assertMatchAndNonMatch(Request(GET, "/").header("Content-Type", "application/json"), hasContentType(APPLICATION_JSON), hasContentType(APPLICATION_FORM_URLENCODED))

    @Test
    fun `body`() = assertMatchAndNonMatch(Request(GET, "/").body("bob"), hasBody("bob"), hasBody("bill"))
}