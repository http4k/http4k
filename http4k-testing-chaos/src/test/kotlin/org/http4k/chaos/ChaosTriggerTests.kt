package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
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
        assertThat(asJson.asA(clazz), equalTo(trigger))
    }

    @Test
    fun `describes itself`() {
        assertThat(trigger(clock).toString(), equalTo(expectedDescription))
    }

    @Test
    abstract fun `behaves as expected`()
}

class DeadlineTriggerTest : SerializableTriggerContract<Deadline>(Deadline::class) {
    override val trigger = Deadline(EPOCH)
    override val expectedJson = """{"endTime":"1970-01-01T00:00:00Z","type":"deadline"}"""
    override val expectedDescription = "Deadline (1970-01-01T00:00:00Z)"

    @Test
    override fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = ChaosTriggers.Deadline(now(clock).plusMillis(100))(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx) shouldMatch equalTo(true)
    }
}

class DelayTriggerTest : SerializableTriggerContract<Delay>(Delay::class) {
    override val trigger = Delay(Duration.ofMillis(100), clock)
    override val expectedJson = """{"endTime":"1970-01-01T00:00:00.100Z","type":"delay"}"""
    override val expectedDescription = "Delay (expires 1970-01-01T00:00:00.100Z)"

    @Test
    override fun `behaves as expected`() {
        val clock = Clock.systemDefaultZone()
        val trigger = ChaosTriggers.Delay(Duration.ofMillis(100), clock)(clock)
        trigger(tx) shouldMatch equalTo(false)
        sleep(110)
        trigger(tx) shouldMatch equalTo(true)
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