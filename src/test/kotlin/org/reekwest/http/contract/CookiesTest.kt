package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.contract.Cookies

class CookiesTest {

//    @Test
//    fun `value present`() {
//        assertThat(Header.optional("hello")(request), equalTo("world"))
//        assertThat(Header.required("hello")(request), equalTo("world"))
//        assertThat(Header.map { it.length }.required("hello")(request), equalTo(5))
//        assertThat(Header.map { it.length }.optional("hello")(request), equalTo(5))
//
//        val expected: List<String?> = listOf("world", "world2")
//        assertThat(Header.multi.required("hello")(request), equalTo(expected))
//        assertThat(Header.multi.optional("hello")(request), equalTo(expected))
//    }
//
//    @Test
//    fun `sets value on request`() {
//        val header = Header.required("bob")
//        val withHeader = header("hello", request)
//        assertThat(header(withHeader), equalTo("hello"))
//    }

    @Test
    fun `toString is ok`() {
        assertThat(Cookies.named("hello").toString(), equalTo("Optional cookie 'hello'"))
    }
}