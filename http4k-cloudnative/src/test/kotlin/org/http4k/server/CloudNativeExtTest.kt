package org.http4k.server

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.cloudnative.env.Port
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test

class CloudNativeExtTest {

    @Test
    fun `can convert HttpHandler to a server with port`() {
        assertThat( HttpHandler { Response(OK) }.asServer(::SunHttp, Port(65412)).port(), equalTo(65412))
    }
}
