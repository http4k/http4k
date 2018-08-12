package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.chaos.ChaosTriggers.MatchResponse
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Jackson
import org.http4k.format.Jackson.asA
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant.EPOCH
import java.time.Instant.now
import java.time.ZoneId.of
import kotlin.reflect.KClass

private val tx = HttpTransaction(Request(GET, ""), Response(OK), ZERO)

abstract class SerializableTriggerContract<T : SerializableTrigger>(private val clazz: KClass<T>) {

    abstract val trigger: T
    abstract val expectedJson: String
    abstract val expectedDescription: String
    protected val clock = Clock.fixed(EPOCH, of("UTC"))

    @Test
    fun `is roundtrippable to JSON`() {
        val asJson = Jackson.asJsonString(trigger)
        assertThat(asJson, equalTo(expectedJson))
        assertThat(asJson.asA(clazz).toString(), equalTo(trigger.toString()))
    }

    @Test
    fun `describes itself`() {
        assertThat(trigger(clock).toString(), equalTo(expectedDescription))
    }
}

class DeadlineTriggerTest : SerializableTriggerContract<Deadline>(Deadline::class) {
    override val trigger = Deadline(EPOCH)
    override val expectedJson = """{"endTime":"1970-01-01T00:00:00Z","type":"deadline"}"""
    override val expectedDescription = "Deadline (1970-01-01T00:00:00Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = ChaosTriggers.Deadline(now(clock).plusMillis(100))(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(200)
        trigger(tx) shouldMatch equalTo(true)
    }
}

class DelayTriggerTest : SerializableTriggerContract<Delay>(Delay::class) {
    override val trigger = Delay(Duration.ofMillis(100), clock)
    override val expectedJson = """{"endTime":"1970-01-01T00:00:00.100Z","type":"delay"}"""
    override val expectedDescription = "Delay (expires 1970-01-01T00:00:00.100Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemDefaultZone()
        val trigger = ChaosTriggers.Delay(Duration.ofMillis(100), clock)(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx) shouldMatch equalTo(true)
    }
}

class MatchRequestTriggerTest : SerializableTriggerContract<MatchRequest>(MatchRequest::class) {
    override val trigger = MatchRequest(Regex(".*bob"),
            mapOf("header" to Regex(".*header")),
            mapOf("query" to Regex(".*query")),
            Regex(".*body"))

    override val expectedJson = """{"path":".*bob","headers":{"header":".*header"},"queries":{"query":".*query"},"body":".*body","type":"request"}"""

    override val expectedDescription = "has Request that has Header 'header' that is not null & matches " +
            ".*header and has Query 'query' that is not null & matches .*query and has Uri " +
            "that has Path that is not null & matches .*bob and has Body that is not null & matches .*body"

    private fun assertMatchNoMatch(s: SerializableTrigger, match: HttpTransaction, noMatch: HttpTransaction) {
        assertThat(s(clock)(match), equalTo(true))
        assertThat(s(clock)(noMatch), equalTo(false))
    }

    @Test
    fun `matches path`() {
        assertMatchNoMatch(MatchRequest(path = Regex(".*bob")),
                tx.copy(Request(GET, "/abob")),
                tx.copy(Request(GET, "/bill")))
    }

    @Test
    fun `matches header`() {
        assertMatchNoMatch(MatchRequest(headers = mapOf("header" to Regex(".*bob"))),
                tx.copy(tx.request.header("header", "abob")),
                tx.copy(tx.request.header("header", "bill")))
    }

    @Test
    fun `matches query`() {
        assertMatchNoMatch(MatchRequest(queries = mapOf("query" to Regex(".*bob"))),
                tx.copy(tx.request.query("query", "abob")),
                tx.copy(tx.request.query("query", "bill")))
    }

    @Test
    fun `matches body`() {
        assertMatchNoMatch(MatchRequest(body = Regex(".*bob")),
                tx.copy(tx.request.body("abob")),
                tx.copy(tx.request.body("bill")))
    }
}

class MatchResponseTriggerTest : SerializableTriggerContract<MatchResponse>(MatchResponse::class) {
    override val trigger = MatchResponse(200,
            mapOf("header" to Regex(".*header")),
            Regex(".*body"))

    override val expectedJson = """{"status":200,"headers":{"header":".*header"},"body":".*body","type":"response"}"""

    override val expectedDescription = "has Response that anything and has Header 'header' that is not null & " +
            "matches .*header and has status that is equal to 200  and has Body that is not null & matches .*body"

    @Test
    fun `behaves as expected`() {


    }
}

class SwitchTriggerTest {
    @Test
    fun `behaves as expected`() {
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
}

class ChaosTriggerTest {
    private val isFalse: ChaosTrigger = { _: HttpTransaction -> false }
    private val isTrue: ChaosTrigger = { _: HttpTransaction -> true }

    @Test
    fun invert() {
        (!isFalse)(tx) shouldMatch equalTo(true)
        (!isTrue)(tx) shouldMatch equalTo(false)
        (!isTrue).toString() shouldMatch equalTo("NOT (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }

    @Test
    fun and() {
        (isFalse and isTrue)(tx) shouldMatch equalTo(false)
        (isTrue and isTrue)(tx) shouldMatch equalTo(true)
        (isTrue and isTrue).toString() shouldMatch equalTo("(org.http4k.core.HttpTransaction) -> kotlin.Boolean AND (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }

    @Test
    fun or() {
        (isFalse or isFalse)(tx) shouldMatch equalTo(false)
        (isFalse or isTrue)(tx) shouldMatch equalTo(true)
        (isTrue or isTrue)(tx) shouldMatch equalTo(true)
        (isTrue or isTrue).toString() shouldMatch equalTo("(org.http4k.core.HttpTransaction) -> kotlin.Boolean OR (org.http4k.core.HttpTransaction) -> kotlin.Boolean")
    }
}