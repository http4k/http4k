package org.http4k.routing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test

class Issue57Test {
    private val resourceLoader = ResourceLoader.Classpath()

    @Test
    fun a() {
        val routes = routes(
            "/path1" bind static(resourceLoader),
            "/path2" bind static(resourceLoader)
        )
        val response1 = routes(Request(GET, "/path1/index.html"))
        val response2 = routes(Request(GET, "/path2/index.html"))
        assertThat("${response1.status.code} ${response2.status.code}", equalTo("200 200"))
    }

    @Test
    fun b() {
        val routes = routes(
            "/path1" bind GET to static(resourceLoader),
            "/path2" bind static(resourceLoader)
        )
        val response1 = routes(Request(GET, "/path1/index.html"))
        val response2 = routes(Request(GET, "/path2/index.html"))
        assertThat("${response1.status.code} ${response2.status.code}", equalTo("200 200"))
    }

    @Test
    fun c() {
        val routes = routes(
            "/path1" bind static(resourceLoader),
            "/path2" bind GET to static(resourceLoader)
        )
        val response1 = routes(Request(GET, "/path1/index.html"))
        val response2 = routes(Request(GET, "/path2/index.html"))
        assertThat("${response1.status.code} ${response2.status.code}", equalTo("200 200"))
    }

    @Test
    fun d() {
        val routes = routes(
            "/path1" bind GET to static(resourceLoader),
            "/path2" bind GET to static(resourceLoader)
        )
        val response1 = routes(Request(GET, "/path1/index.html"))
        val response2 = routes(Request(GET, "/path2/index.html"))
        assertThat("${response1.status.code} ${response2.status.code}", equalTo("200 200"))
    }
}
