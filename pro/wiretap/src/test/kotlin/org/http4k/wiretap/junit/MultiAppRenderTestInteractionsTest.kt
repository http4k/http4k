package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MultiAppRenderTestInteractionsTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val wiretap = RenderTestInteractions(
        {
            val inner = App(http(), otel("test app 2"), "test app 2")
            App(inner, otel("test app 1"), "test app 1")
        }, downstream,
        Always
    )

    @Test
    fun `requests through factory-built app reach the app`(http: HttpHandler) {
        val response = http(testRequest())
        assertThat(response.bodyString(), equalTo("downstream"))
    }
}
