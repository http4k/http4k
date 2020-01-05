package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

class Client(private val http: HttpHandler) {
    fun someMethod(): String = http(Request(GET, "/")).bodyString()
}

interface ClientContract : ServirtiumContract {
    @JvmDefault
    override val name get() = "Recording"

    @Test
    @JvmDefault
    fun `foo bar`(handler: HttpHandler) {
        assertThat(Client(handler).someMethod(), equalTo("some value"))
    }
}

@Disabled
class RecordingClientTest : ClientContract {
    private val app = { _: Request -> Response(OK).body("some value") }

    @JvmField
    @RegisterExtension
    val record = ServirtiumRecording(name, app)
}

@Disabled
class VerifyRecordedClientContractTest : VerifyServirtiumContract(File("."), ClientContract::class.java.name)