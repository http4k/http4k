package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.Missing
import org.reekwest.http.core.contract.Query
import org.reekwest.http.core.contract.get

class QueryTest {
    private val request = Request(GET, uri("/?hello=world&hello=world2"))

    @Test
    fun `value present`() {
        assertThat(request[Query.optional("hello")], equalTo("world"))
        assertThat(request[Query.required("hello")], equalTo("world"))
        assertThat(request[Query.required("hello").map { it.length }], equalTo(5))
        assertThat(request[Query.optional("hello").map { it.length }], equalTo(5))

        val expected: List<String?> = listOf("world", "world2")
        assertThat(request[Query.multi.required("hello")], equalTo(expected))
        assertThat(request[Query.multi.optional("hello")], equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(request[Query.optional("world")], absent())
        assertThat({ request[Query.required("world")] }, throws<Missing>())

        assertThat(request[Query.multi.optional("world")], equalTo(emptyList()))
        assertThat({ request[Query.multi.required("world")] }, throws<Missing>())
    }

    @Test
    fun `invalid value`() {
        assertThat({ request[Query.required("hello").map { it.toInt() }] }, throws<Invalid>())
        assertThat({ request[Query.optional("hello").map { it.toInt() }] }, throws<Invalid>())

        assertThat({ request[Query.multi.required("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
        assertThat({ request[Query.multi.optional("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
    }
}