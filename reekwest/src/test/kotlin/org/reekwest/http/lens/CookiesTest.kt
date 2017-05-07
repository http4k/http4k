package org.reekwest.http.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.cookie.Cookie
import org.reekwest.http.core.cookie.cookie

class CookiesTest {
    private val request = Request(GET, uri(""))
        .cookie("hello", "world")
        .cookie("hello", "world2")

    @Test
    fun `value present`() {
        assertThat(Cookies.optional("hello")(request), equalTo(Cookie("hello", "world")))
        assertThat(Cookies.required("hello")(request), equalTo(Cookie("hello", "world")))
        assertThat(Cookies.map { it.value.length }.required("hello")(request), equalTo(5))
        assertThat(Cookies.map { it.value.length }.optional("hello")(request), equalTo(5))

        val expected: List<Cookie?> = listOf(Cookie("hello", "world"), Cookie("hello", "world2"))
        assertThat(Cookies.multi.required("hello")(request), equalTo(expected))
        assertThat(Cookies.multi.optional("hello")(request), equalTo(expected))
    }

    @Test
    fun `value missing`() {
        assertThat(Cookies.optional("world")(request), absent())
        val required = Cookies.required("world")
        assertThat({ required(request) }, throws(equalTo(LensFailure(required.missing()))))

        assertThat(Cookies.multi.optional("world")(request), absent())
        val optionalMulti = Cookies.multi.required("world")
        assertThat({ optionalMulti(request) }, throws(equalTo(LensFailure(optionalMulti.missing()))))
    }

    @Test
    fun `invalid value`() {
        val asInt = Cookies.map { it.value.toInt() }

        val required = asInt.required("hello")
        assertThat({ required(request) }, throws(equalTo(LensFailure(required.invalid()))))

        val optional = asInt.optional("hello")
        assertThat({ optional(request) }, throws(equalTo(LensFailure(optional.invalid()))))

        val requiredMulti = asInt.multi.required("hello")
        assertThat({ requiredMulti(request) }, throws(equalTo(LensFailure(requiredMulti.invalid()))))

        val optionalMulti = asInt.multi.optional("hello")
        assertThat({ optionalMulti(request) }, throws(equalTo(LensFailure(optionalMulti.invalid()))))
    }

    @Test
    fun `sets value on request`() {
        val cookie = Cookies.required("bob")
        val cookieInstance = Cookie("bob", "hello")
        val withCookies = cookie(cookieInstance, request)
        assertThat(cookie(withCookies), equalTo(cookieInstance))
    }

    @Test
    fun `can create a custom type and get and set on request`() {
        val custom = Cookies.map({ MyCustomBodyType(it.value) }, { Cookie("bob", it.value) }).required("bob")

        val instance = MyCustomBodyType("hello world!")
        val reqWithCookies = custom(instance, get(""))

        assertThat(reqWithCookies.cookie("bob"), equalTo(Cookie("bob", "hello world!")))

        assertThat(custom(reqWithCookies), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Cookies.optional("hello").toString(), equalTo("Optional cookie 'hello'"))
    }
}