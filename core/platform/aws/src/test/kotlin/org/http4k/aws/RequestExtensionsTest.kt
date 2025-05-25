package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test


class RequestExtensionsTest {

    @Test
    fun `encodes path`() = runBlocking {
        val encodedRequest = Request(GET, "http://www.google.com/a:b:c/d e/*f/~g").encodeUri()
        assertThat(
            encodedRequest.uri.path,
            equalTo("/a%3Ab%3Ac/d%20e/%2Af/~g")
        )
    }
}
