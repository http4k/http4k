package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class Client(private val http: HttpHandler) {
    fun getThing(): String = http(Request(GET, "/")).bodyString()
}

open class ClientContract : ServirtiumContract("Recording") {
    @Test
    fun `foo bar`(handler: HttpHandler) {
        assertThat(Client(handler).getThing(), equalTo("some value"))
    }
}

class RecordingClientTest : ClientContract() {
    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording(name) { Response(OK).body("some value") }
}

