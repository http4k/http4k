package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosTriggers.Countdown
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson.asJsonObject
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant.EPOCH
import java.time.ZoneId.of

private val tx = HttpTransaction(Request(GET, ""), Response(OK), ZERO)

abstract class ChaosTriggerContract {
    abstract val asJson: String
    abstract val expectedDescription: String

    @Test
    fun `deserialises from JSON`() {
        val clock = Clock.fixed(EPOCH, of("UTC"))
        asJson.asJsonObject().asTrigger(clock).toString() shouldMatch equalTo(expectedDescription)
    }
}

class DeadlineTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"deadline","endTime":"1970-01-01T00:00:00Z"}"""
    override val expectedDescription = "Deadline (1970-01-01T00:00:00Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = Deadline(clock.instant().plusMillis(100), clock)
        trigger(tx.request) shouldMatch equalTo(false)
        sleep(200)
        trigger(tx.request) shouldMatch equalTo(true)
    }
}

class CounterTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"countdown","count":"1"}"""
    override val expectedDescription = "Countdown (1 remaining)"

    @Test
    fun `behaves as expected`() {
        val trigger = Countdown(1)
        trigger(tx.request) shouldMatch equalTo(true)
        trigger(tx.request) shouldMatch equalTo(false)
        trigger(tx.request) shouldMatch equalTo(false)
    }
}

class DelayTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"delay","period":"PT0.1S"}"""
    override val expectedDescription = "Delay (expires 1970-01-01T00:00:00.100Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = Delay(Duration.ofMillis(100), clock)
        trigger(tx.request) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx.request) shouldMatch equalTo(true)
    }
}

class MatchRequestTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"request","method":"get","path":".*bob","queries":{"query":".*query"},"headers":{"header":".*header"},"body":".*body"}"""

    override val expectedDescription = "has Request that has Method that is equal to GET and has Uri that has Path " +
            "that is not null & matches .*bob and has Query 'query' that is not null & matches .*query and has Header" +
            " 'header' that is not null & matches .*header and has Body that is not null & matches .*body"

    private fun assertMatchNoMatch(s: Trigger, match: Request, noMatch: Request) {
        assertThat(s(match), equalTo(true))
        assertThat(s(noMatch), equalTo(false))
    }

    @Test
    fun `matches path`() {
        assertMatchNoMatch(MatchRequest(path = Regex(".*bob")),
                Request(GET, "/abob"),
                Request(GET, "/bill"))
    }

    @Test
    fun `matches header`() {
        assertMatchNoMatch(MatchRequest(headers = mapOf("header" to Regex(".*bob"))),
                tx.request.header("header", "abob"),
                tx.request.header("header", "bill"))
    }

    @Test
    fun `matches query`() {
        assertMatchNoMatch(MatchRequest(queries = mapOf("query" to Regex(".*bob"))),
                tx.request.query("query", "abob"),
                tx.request.query("query", "bill"))
    }

    @Test
    fun `matches body`() {
        assertMatchNoMatch(MatchRequest(body = Regex(".*bob")),
                tx.request.body("abob"),
                tx.request.body("bill"))
    }

    @Test
    fun `matches method`() {
        assertMatchNoMatch(MatchRequest(method = "put"),
                Request(PUT, "/abob"),
                Request(GET, "/abob"))
    }
}

class SwitchTriggerTest {
    @Test
    fun `behaves as expected`() {
        val trigger = SwitchTrigger(true)
        trigger.toString() shouldMatch equalTo("SwitchTrigger (active = true)")
        trigger(tx.request) shouldMatch equalTo(true)
        trigger.toggle()
        trigger(tx.request) shouldMatch equalTo(false)
        trigger.toggle(false)
        trigger(tx.request) shouldMatch equalTo(false)
        trigger.toggle(true)
        trigger(tx.request) shouldMatch equalTo(true)
    }
}

class ChaosTriggerLogicalOperatorTest {
    private val isFalse: Trigger = { _: Request -> false }
    private val isTrue: Trigger = { _: Request -> true }

    @Test
    fun invert() {
        (!isFalse)(tx.request) shouldMatch equalTo(true)
        (!isTrue)(tx.request) shouldMatch equalTo(false)
        (!isTrue).toString() shouldMatch equalTo("NOT (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }

    @Test
    fun and() {
        (isFalse and isTrue)(tx.request) shouldMatch equalTo(false)
        (isTrue and isTrue)(tx.request) shouldMatch equalTo(true)
        (isTrue and isTrue).toString() shouldMatch equalTo("(org.http4k.core.HttpTransaction) -> kotlin.Boolean AND (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }

    @Test
    fun or() {
        (isFalse or isFalse)(tx.request) shouldMatch equalTo(false)
        (isFalse or isTrue)(tx.request) shouldMatch equalTo(true)
        (isTrue or isTrue)(tx.request) shouldMatch equalTo(true)
        (isTrue or isTrue).toString() shouldMatch equalTo("(org.http4k.core.HttpTransaction) -> kotlin.Boolean OR (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }
}