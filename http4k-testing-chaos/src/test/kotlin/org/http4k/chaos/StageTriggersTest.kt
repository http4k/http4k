package org.http4k.chaos

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant

class StageTriggersTest {
    private val tx = HttpTransaction(Request(GET, ""), Response(OK), ZERO)

    @Test
    fun `deadline trigger`() {
        val clock = Clock.systemDefaultZone()
        val trigger = StageTriggers.Deadline(Instant.now().plusMillis(100), clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(100)
        trigger(tx) shouldMatch equalTo(true)
    }

    @Test
    fun `delay trigger`() {
        val clock = Clock.systemDefaultZone()
        val trigger = StageTriggers.Delay(Duration.ofMillis(100), clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(100)
        trigger(tx) shouldMatch equalTo(true)
    }
}