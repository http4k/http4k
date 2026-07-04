package org.http4k.filter.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.junit.jupiter.api.Test
import java.time.Instant

class DefaultCookieStorageTest {

    private val now = Instant.EPOCH
    private val storage = DefaultCookieStorage()

    private fun store(cookie: Cookie, origin: String) =
        storage.store(listOf(LocalCookie(cookie, now, Uri.of(origin))))

    private fun cookiesFor(uri: String) =
        storage.retrieve(Uri.of(uri)).map { it.cookie.name }

    @Test
    fun `host-only cookie is sent back to exact origin host`() {
        store(Cookie("sid", "abc"), "https://a.example.com/")
        assertThat(cookiesFor("https://a.example.com/"), equalTo(listOf("sid")))
    }

    @Test
    fun `host-only cookie is NOT sent to a different host`() {
        store(Cookie("sid", "abc"), "https://a.example.com/")
        assertThat(cookiesFor("https://b.example.com/"), isEmpty)
    }

    @Test
    fun `host-only cookie is NOT sent to a subdomain of the origin`() {
        store(Cookie("sid", "abc"), "https://example.com/")
        assertThat(cookiesFor("https://sub.example.com/"), isEmpty)
    }

    @Test
    fun `domain cookie is sent to exact domain`() {
        store(Cookie("sess", "x", domain = "example.com"), "https://example.com/")
        assertThat(cookiesFor("https://example.com/"), equalTo(listOf("sess")))
    }

    @Test
    fun `domain cookie is sent to subdomain`() {
        store(Cookie("sess", "x", domain = "example.com"), "https://example.com/")
        assertThat(cookiesFor("https://sub.example.com/"), equalTo(listOf("sess")))
    }

    @Test
    fun `domain cookie with leading dot is sent to subdomain`() {
        store(Cookie("sess", "x", domain = ".example.com"), "https://example.com/")
        assertThat(cookiesFor("https://api.example.com/"), equalTo(listOf("sess")))
    }

    @Test
    fun `domain cookie is NOT sent to a sibling domain`() {
        store(Cookie("sess", "x", domain = "a.example.com"), "https://a.example.com/")
        assertThat(cookiesFor("https://b.example.com/"), isEmpty)
    }

    @Test
    fun `domain cookie is NOT sent to a domain that merely ends with the cookie domain string`() {
        store(Cookie("sess", "x", domain = "evil.com"), "https://evil.com/")
        // notevil.com should not match
        assertThat(cookiesFor("https://notevil.com/"), isEmpty)
    }

    @Test
    fun `cookie whose Domain does not match the origin host is rejected (cookie tossing)`() {
        store(Cookie("evil", "x", domain = "victim.com"), "https://evil.com/")
        assertThat(cookiesFor("https://victim.com/"), isEmpty)
    }

    @Test
    fun `cookie with a dotless public-suffix Domain is rejected`() {
        store(Cookie("evil", "x", domain = "com"), "https://evil.com/")
        assertThat(cookiesFor("https://anything.com/"), isEmpty)
    }

    @Test
    fun `localhost cookie with Domain=localhost is accepted`() {
        store(Cookie("sid", "x", domain = "localhost"), "http://localhost/")
        assertThat(cookiesFor("http://localhost/"), equalTo(listOf("sid")))
    }

    @Test
    fun `registrable domain cookie under a multi-label public suffix is accepted`() {
        store(Cookie("sess", "x", domain = "example.co.uk"), "https://www.example.co.uk/")
        assertThat(cookiesFor("https://www.example.co.uk/"), equalTo(listOf("sess")))
    }

    @Test
    fun `cookie with path is sent to exact path`() {
        store(Cookie("tok", "y", path = "/api"), "https://example.com/api")
        assertThat(cookiesFor("https://example.com/api"), equalTo(listOf("tok")))
    }

    @Test
    fun `cookie with path is sent to child path`() {
        store(Cookie("tok", "y", path = "/api"), "https://example.com/api")
        assertThat(cookiesFor("https://example.com/api/v1"), equalTo(listOf("tok")))
    }

    @Test
    fun `cookie with path is NOT sent to sibling path`() {
        store(Cookie("tok", "y", path = "/api"), "https://example.com/api")
        assertThat(cookiesFor("https://example.com/other"), isEmpty)
    }

    @Test
    fun `cookie with path is NOT sent when request path is a prefix but without slash separator`() {
        // /apiv2 should not match /api
        store(Cookie("tok", "y", path = "/api"), "https://example.com/api")
        assertThat(cookiesFor("https://example.com/apiv2"), isEmpty)
    }

    @Test
    fun `secure cookie is sent over https`() {
        store(Cookie("cred", "z", secure = true), "https://example.com/")
        assertThat(cookiesFor("https://example.com/"), equalTo(listOf("cred")))
    }

    @Test
    fun `secure cookie is NOT sent over http`() {
        store(Cookie("cred", "z", secure = true), "https://example.com/")
        assertThat(cookiesFor("http://example.com/"), isEmpty)
    }

    @Test
    fun `non-secure cookie is sent over http`() {
        store(Cookie("plain", "p"), "http://example.com/")
        assertThat(cookiesFor("http://example.com/"), equalTo(listOf("plain")))
    }

    @Test
    fun `cookies from two different hosts do not leak to each other`() {
        store(Cookie("tokenA", "fromA"), "https://site-a.com/")
        store(Cookie("tokenB", "fromB"), "https://site-b.com/")

        assertThat(cookiesFor("https://site-a.com/"), equalTo(listOf("tokenA")))
        assertThat(cookiesFor("https://site-b.com/"), equalTo(listOf("tokenB")))
    }

    @Test
    fun `same cookie name from two different origins are stored independently`() {
        store(Cookie("sid", "valueA"), "https://site-a.com/")
        store(Cookie("sid", "valueB"), "https://site-b.com/")

        val forA = storage.retrieve(Uri.of("https://site-a.com/"))
        val forB = storage.retrieve(Uri.of("https://site-b.com/"))
        assertThat(forA.single().cookie.value, equalTo("valueA"))
        assertThat(forB.single().cookie.value, equalTo("valueB"))
    }

    @Test
    fun `remove deletes all cookies with that name across all origins`() {
        store(Cookie("sid", "a"), "https://a.com/")
        store(Cookie("sid", "b"), "https://b.com/")
        storage.remove("sid")
        assertThat(storage.retrieve(Uri.of("https://a.com/")), isEmpty)
        assertThat(storage.retrieve(Uri.of("https://b.com/")), isEmpty)
    }
}
