package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Request.Companion.get
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.junit.Test

abstract class Http4kClientContract {
    abstract val client: HttpHandler

    @Test
    fun `performs simple request`() {
        val response = client(get("http://httpbin.org/get").query("name", "John Doe"))

        assertThat(response.status, equalTo(Status.OK))
        assertThat(response.bodyString(), containsSubstring("John Doe"))
    }

    @Test
    fun `does not follow redirects`() {
        val response = client(get("http://httpbin.org/redirect-to").query("url", "/destination"))

        assertThat(response.status, equalTo(Status.FOUND))
        assertThat(response.header("location"), equalTo("/destination"))
    }

    @Test
    fun `does not store cookies`() {
        client(get("http://httpbin.org/cookies/set").query("foo", "bar"))

        val response = client(get("http://httpbin.org/cookies"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), !containsSubstring("foo"))
    }

    @Test
    fun `filters enable cookies and redirects`() {
        val enhancedClient = ClientFilters.FollowRedirects().then(ClientFilters.Cookies()).then(client)

        val response = enhancedClient(get("http://httpbin.org/cookies/set").query("foo", "bar"))

        assertThat(response.status.successful, equalTo(true))
        assertThat(response.bodyString(), containsSubstring("foo"))
    }
}