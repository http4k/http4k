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
    private val request = Request(GET, uri("/"), listOf("hello" to "world"))

    @Test
    fun `retrieve successfully`() {
        assertThat(request[Header.optional("hello")], equalTo("world"))
        assertThat(request[Header.required("hello")], equalTo("world"))
        assertThat(request[Header.required("hello").map { it.length }], equalTo(5))
        assertThat(request[Header.optional("hello").map { it!!.length }], equalTo(5))
    }

    @Test
    fun `retrieve fails with missing`() {
        assertThat(request[Header.optional("world")], absent())
        assertThat({ request[Header.required("world")] }, throws<Missing>())
    }

    @Test
    fun `retrieve fails with invalid`() {
        assertThat({ request[Header.required("hello").map { it.toInt() }] }, throws<Invalid>())
        assertThat({ request[Header.optional("world").map { it!!.toInt() }] }, throws<Invalid>())
    }


}