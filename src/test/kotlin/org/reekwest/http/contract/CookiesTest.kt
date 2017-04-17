package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Ignore
import org.junit.Test
import org.reekwest.http.core.contract.Cookies
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.get

class CookiesTest {

    @Test
    @Ignore
    fun `value present`() {
        assertThat(Cookies.optional("hello")(get("")), equalTo(Cookie("hello", "world")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Cookies.optional("hello").toString(), equalTo("Optional cookie 'hello'"))
    }
}