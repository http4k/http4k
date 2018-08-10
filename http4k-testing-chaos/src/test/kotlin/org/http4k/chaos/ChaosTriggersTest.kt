package org.http4k.chaos

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant.EPOCH
import java.time.Instant.now
import java.time.ZoneId.of

class ChaosTriggersTest {
    private val tx = HttpTransaction(Request(GET, ""), Response(OK), ZERO)

    private val dayDot = Clock.fixed(EPOCH, of("UTC"))

    @Test
    fun `deadline trigger`() {
        ChaosTriggers.Deadline(now(dayDot))(dayDot).toString() shouldMatch equalTo("Deadline (1970-01-01T00:00:00Z)")

        val clock = Clock.systemDefaultZone()
        val trigger = ChaosTriggers.Deadline(now(clock).plusMillis(100))(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx) shouldMatch equalTo(true)
    }

    @Test
    fun `delay trigger`() {
        ChaosTriggers.Delay(Duration.ofMillis(100), dayDot)(dayDot).toString() shouldMatch equalTo("Delay (expires 1970-01-01T00:00:00.100Z)")

        val clock = Clock.systemDefaultZone()
        val trigger = ChaosTriggers.Delay(Duration.ofMillis(100), clock)(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx) shouldMatch equalTo(true)
    }

    @Test
    fun `switch trigger`() {
        val trigger = SwitchTrigger(true)
        trigger.toString() shouldMatch equalTo("SwitchTrigger (active = true)")
        trigger(tx) shouldMatch equalTo(true)
        trigger.toggle()
        trigger(tx) shouldMatch equalTo(false)
        trigger.toggle(false)
        trigger(tx) shouldMatch equalTo(false)
        trigger.toggle(true)
        trigger(tx) shouldMatch equalTo(true)
    }

    @Test
    fun `inversion of trigger`() {
        (!{ _: HttpTransaction -> false })(tx) shouldMatch equalTo(true)
        (!{ _: HttpTransaction -> true })(tx) shouldMatch equalTo(false)
        (!{ _: HttpTransaction -> true }).toString() shouldMatch equalTo("NOT (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }

}