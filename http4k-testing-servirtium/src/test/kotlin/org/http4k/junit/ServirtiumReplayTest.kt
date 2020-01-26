package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.ServirtiumContract
import org.http4k.util.proxy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.Optional

class ServirtiumReplayTest {

    class Stub(private val t: Any) : ExtensionContext by proxy(), ParameterContext by proxy() {
        override fun getTestInstance() = Optional.of(t)
        override fun getTestMethod() = Optional.of(ServirtiumReplayTest::class.java.getMethod("hashCode"))
    }

    object AContract : ServirtiumContract {
        override val name get() = "name"
    }

    @TempDir
    lateinit var root: File

    @Test
    fun `replays traffic from the recording`() {
        javaClass.getResourceAsStream("/org/http4k/junit/storedTraffic.txt").reader().use { r ->
            File(root, "name.hashCode.md").writer().use { r.copyTo(it) }
        }

        val stub = Stub(AContract)

        val originalRequest = Request(POST, "/foo")
            .header("header1", "value1")
            .body("body")

        val expectedResponse = Response(OK)
            .header("header3", "value3")
            .body("body1")

        val servirtiumReplay = ServirtiumReplay(root) {
            it.header("toBeAdded", "value")
        }
        @Suppress("UNCHECKED_CAST")
        val actualResponse = (servirtiumReplay.resolveParameter(stub, stub) as HttpHandler)(originalRequest)

        assertThat(actualResponse, equalTo(expectedResponse))
    }
}
