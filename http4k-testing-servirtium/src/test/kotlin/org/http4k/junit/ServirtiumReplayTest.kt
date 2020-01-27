package org.http4k.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.servirtium.ServirtiumContract
import org.http4k.servirtium.StorageFactory.Companion.Disk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ServirtiumReplayTest {


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

        val stub = JUnitStub(AContract)

        val originalRequest = Request(POST, "/foo")
            .header("header1", "value1")
            .body("body")

        val expectedResponse = Response(OK)
            .header("header3", "value3")
            .body("body1")

        val servirtiumReplay = ServirtiumReplay(Disk(root)) {
            it.header("toBeAdded", "value")
        }
        @Suppress("UNCHECKED_CAST")
        val actualResponse = (servirtiumReplay.resolveParameter(stub, stub) as HttpHandler)(originalRequest)

        assertThat(actualResponse, equalTo(expectedResponse))
    }
}
