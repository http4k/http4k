package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.contract.Header
import org.reekwest.http.core.contract.Invalid
import org.reekwest.http.core.contract.Missing
import org.reekwest.http.core.contract.get

class HeaderTest {
    private val request = Request(GET, uri("/"), listOf("hello" to "world", "hello" to "world2"))

    @Test
    fun `value present`() {
        assertThat(request[Header.optional("hello")], equalTo("world"))
        assertThat(request[Header.required("hello")], equalTo("world"))
        assertThat(request[Header.map { it.length }.required("hello")], equalTo(5))
        assertThat(request[Header.map { it.length }.optional("hello")], equalTo(5))

//        val expected: List<String?> = listOf("world", "world2")
//        assertThat(request[Header.multi.required("hello")], equalTo(expected))
//        assertThat(request[Header.multi.optional("hello")], equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(request[Header.optional("world")], absent())
        assertThat({ request[Header.required("world")] }, throws<Missing>())

//        assertThat(request[Header.multi.optional("world")], equalTo(emptyList()))
//        assertThat({ request[Header.multi.required("world")] }, throws<Missing>())
    }

    @Test
    fun `invalid value`() {
        assertThat({ request[Header.map { it.toInt() }.required("hello")] }, throws<Invalid>())
        assertThat({ request[Header.map { it.toInt() }.optional("hello")] }, throws<Invalid>())

//        assertThat({ request[Header.multi.required("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
//        assertThat({ request[Header.multi.optional("hello").map { it.map { it?.toInt() } }] }, throws<Invalid>())
    }

    @Test
    fun `toString is ok`() {
//        assertThat(Header.required("hello").toString(), equalTo("Required header 'hello'"))
//        assertThat(Header.optional("hello").toString(), equalTo("Optional header 'hello'"))
//        assertThat(Header.multi.required("hello").toString(), equalTo("Required header 'hello'"))
//        assertThat(Header.multi.optional("hello").toString(), equalTo("Optional header 'hello'"))
    }

}