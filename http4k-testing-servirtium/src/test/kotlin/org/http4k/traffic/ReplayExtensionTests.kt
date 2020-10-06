package org.http4k.traffic

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.traffic.Replay.Companion.MemoryStream
import org.junit.jupiter.api.Test

class ReplayExtensionTests {
    private val request = Request(GET, "/")
    private val response = Response(OK)
    private val http = MemoryStream(mutableListOf(request.header("toBeAdded", "value") to response)).replayingMatchingContent {
        it.header("toBeAdded", "value").body(it.bodyString() + it.bodyString())
    }

    @Test
    fun `replayingMatchingContent replays matching content ok, applying the manipulations to make request match`() {
        assertThat(http(request), equalTo(response))
    }

    @Test
    fun `replayingMatchingContent blows up with non-matching request`() {
        assertThat(
            http(Request(GET, "/foobar")),
            hasStatus(NOT_IMPLEMENTED).and(
                hasBody(containsSubstring("Unexpected request received for Interaction 0 ==>"))
            )
        )
    }

    @Test
    fun `replayingMatchingContent blows up when more requests than interactions`() {
        assertThat(http(request), equalTo(response))
        assertThat(
            http(request),
            hasStatus(NOT_IMPLEMENTED).and(
                hasBody(containsSubstring("Have 1 interaction(s) in the script but called 2 times. Unexpected interaction"))
            )
        )
    }
}
