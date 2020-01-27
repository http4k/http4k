package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.InteractionOptions
import org.http4k.servirtium.InteractionStorage.Companion.InMemory
import org.junit.jupiter.api.Test

class ServirtiumReplayTest {

    object AContract

    private val storage = InMemory()

    @Test
    fun `replays traffic from the recording`() {
        javaClass.getResourceAsStream("/org/http4k/junit/storedTraffic.txt").reader().use { r ->
            storage("name.hashCode").accept(r.readText().toByteArray())
        }

        val stub = JUnitStub(AContract)

        val originalRequest = Request(POST, "/foo")
            .header("header1", "value1")
            .body("body")

        val expectedResponse = Response(OK)
            .header("header3", "value3")
            .body("body1")

        val servirtiumReplay = ServirtiumReplay("name", storage,
            object : InteractionOptions {
                override fun modify(request: Request): Request = request.header("toBeAdded", "value")
            })

        @Suppress("UNCHECKED_CAST")
        val actualResponse = (servirtiumReplay.resolveParameter(stub, stub) as HttpHandler)(originalRequest)

        assertThat(actualResponse, equalTo(expectedResponse))
    }
}
