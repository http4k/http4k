package org.reekwest.kontrakt

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
        val requiredCookies = Cookies.required("world")
        assertThat({ requiredCookies(request) }, throws(equalTo(ContractBreach(Missing(requiredCookies)))))

        assertThat(Cookies.multi.optional("world")(request), absent())
        val optionalMultiCookies = Cookies.multi.required("world")
        assertThat({ optionalMultiCookies(request) }, throws(equalTo(ContractBreach(Missing(optionalMultiCookies)))))
    }

    @Test
    fun `invalid value`() {
        val asInt = Cookies.map { it.value.toInt() }

        val requiredCookies = asInt.required("hello")
        assertThat({ requiredCookies(request) }, throws(equalTo(ContractBreach(Invalid(requiredCookies)))))

        val optionalCookies = asInt.optional("hello")
        assertThat({ optionalCookies(request) }, throws(equalTo(ContractBreach(Invalid(optionalCookies)))))

        val requiredMultiCookies = asInt.multi.required("hello")
        assertThat({ requiredMultiCookies(request) }, throws(equalTo(ContractBreach(Invalid(requiredMultiCookies)))))

        val optionalMultiCookies = asInt.multi.optional("hello")
        assertThat({ optionalMultiCookies(request) }, throws(equalTo(ContractBreach(Invalid(optionalMultiCookies)))))
    }

    @Test
    fun `sets value on request`() {
        val cookie = Cookies.required("bob")
        val cookieInstance = Cookie("bob", "hello")
        val withCookies = cookie(cookieInstance, request)
        println(withCookies)
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