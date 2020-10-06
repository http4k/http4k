package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosTriggers.Always
import org.http4k.chaos.ChaosTriggers.Countdown
import org.http4k.chaos.ChaosTriggers.Deadline
import org.http4k.chaos.ChaosTriggers.Delay
import org.http4k.chaos.ChaosTriggers.MatchRequest
import org.http4k.chaos.ChaosTriggers.Once
import org.http4k.chaos.ChaosTriggers.PercentageBased
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.format.Jackson.asJsonObject
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Clock
import java.time.Duration
import java.time.Instant.EPOCH
import java.time.ZoneId.of
import java.util.Properties

private val request = Request(GET, "")

abstract class ChaosTriggerContract {
    abstract val asJson: String
    abstract val expectedDescription: String

    @Test
    fun `deserialises from JSON`() {
        val clock = Clock.fixed(EPOCH, of("UTC"))
        assertThat(asJson.asJsonObject().asTrigger(clock).toString(), equalTo(expectedDescription))
    }
}

class DeadlineTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"deadline","endTime":"1970-01-01T00:00:00Z"}"""
    override val expectedDescription = "Deadline (1970-01-01T00:00:00Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = Deadline(clock.instant().plusMillis(100), clock)
        assertThat(trigger(request), equalTo(false))
        sleep(200)
        assertThat(trigger(request), equalTo(true))
    }
}

class CountdownTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"countdown","count":"1"}"""
    override val expectedDescription = "Countdown (1 remaining)"

    @Test
    fun `behaves as expected`() {
        val trigger = Countdown(2)
        assertThat(trigger(request), equalTo(false))
        assertThat(trigger(request), equalTo(false))
        assertThat(trigger(request), equalTo(true))
        assertThat(trigger(request), equalTo(true))
    }
}

class DelayTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"delay","period":"PT0.1S"}"""
    override val expectedDescription = "Delay (expires 1970-01-01T00:00:00.100Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = Delay(Duration.ofMillis(500), clock)
        assertThat(trigger(request), equalTo(false))
        sleep(1000)
        assertThat(trigger(request), equalTo(true))
    }
}

class MatchRequestTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"request","method":"get","path":".*bob","queries":{"query":".*query"},"headers":{"header":".*header"},"body":".*body"}"""

    override val expectedDescription = "has Method that is equal to GET and has Uri that has Path " +
        "that is not null & matches .*bob and has Query 'query' that is not null & matches .*query and has Header" +
        " 'header' that is not null & matches .*header and has Body that is not null & matches .*body"

    private fun assertMatchNoMatch(s: Trigger, match: Request, noMatch: Request) {
        assertThat(s(match), equalTo(true))
        assertThat(s(noMatch), equalTo(false))
    }

    @Test
    fun `matches path`() {
        assertMatchNoMatch(
            MatchRequest(path = Regex(".*bob")),
            Request(GET, "/abob"),
            Request(GET, "/bill")
        )
    }

    @Test
    fun `matches header`() {
        assertMatchNoMatch(
            MatchRequest(headers = mapOf("header" to Regex(".*bob"))),
            request.header("header", "abob"),
            request.header("header", "bill")
        )
    }

    @Test
    fun `matches query`() {
        assertMatchNoMatch(
            MatchRequest(queries = mapOf("query" to Regex(".*bob"))),
            request.query("query", "abob"),
            request.query("query", "bill")
        )
    }

    @Test
    fun `matches body`() {
        assertMatchNoMatch(
            MatchRequest(body = Regex(".*bob")),
            request.body("abob"),
            request.body("bill")
        )
    }

    @Test
    fun `matches method`() {
        assertMatchNoMatch(
            MatchRequest(method = "put"),
            Request(PUT, "/abob"),
            Request(GET, "/abob")
        )
    }
}

class AlwaysTest : ChaosTriggerContract() {
    override val asJson = """{"type":"always"}"""

    override val expectedDescription = "Always"

    @Test
    fun `Always applies by default`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Always()).asFilter().then { Response(OK) }
        assertThat(http(Request(GET, "/foo")), hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500")))
    }
}

class PercentageBasedTest : ChaosTriggerContract() {
    override val asJson = """{"type":"percentage", "percentage":100}"""

    override val expectedDescription = "PercentageBased (100%)"

    @Test
    fun `from environment`() {
        assertThat(PercentageBased.fromEnvironment({ Properties().apply { put("CHAOS_PERCENTAGE", "75") }.getProperty(it) }).toString(), equalTo("PercentageBased (75%)"))
        assertThat(PercentageBased.fromEnvironment().toString(), equalTo("PercentageBased (50%)"))
    }

    @Test
    fun `PercentageBased applied`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(PercentageBased(100)).asFilter().then { Response(OK) }
        assertThat(http(Request(GET, "/foo")), hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500")))
    }
}

class OnceTest : ChaosTriggerContract() {
    override val asJson = """{"type":"once","trigger":{"type":"deadline","endTime":"1970-01-01T00:00:00Z"}}"""

    override val expectedDescription = "Once (trigger = Deadline (1970-01-01T00:00:00Z))"

    @Test
    fun `deserialises from JSON no trigger`() {
        val clock = Clock.fixed(EPOCH, of("UTC"))
        assertThat("""{"type":"once"}""".asJsonObject().asTrigger(clock).toString(), equalTo("Once"))
    }

    @Test
    fun `Once only fires once with trigger`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Once { it.method == GET }).asFilter().then { Response(OK) }
        assertThat(http(Request(POST, "/foo")), hasStatus(OK))
        assertThat(http(Request(GET, "/foo")), hasStatus(INTERNAL_SERVER_ERROR))
        assertThat(http(Request(POST, "/foo")), hasStatus(OK))
        assertThat(http(Request(GET, "/foo")), hasStatus(OK))
    }
}

class ChaosPolicyOperationTest {

    @Test
    fun `Until stops a behaviour when triggered`() {
        val stage: Stage = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Always()).until { it.method == POST }
        assertThat(stage.toString(), equalTo("Always ReturnStatus (500) until (org.http4k.core.Request) -> kotlin.Boolean"))

        val http: HttpHandler = stage.asFilter().then { Response(OK) }

        assertThat(http(Request(GET, "/foo")), hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500")))
        assertThat(http(Request(POST, "/bar")), hasStatus(OK).and(!hasHeader("x-http4k-chaos")))
        assertThat(http(Request(GET, "/bar")), hasStatus(OK).and(!hasHeader("x-http4k-chaos")))
    }
}

class ChaosTriggerLogicalOperatorTest {
    private val isFalse: Trigger = { false }
    private val isTrue: Trigger = { true }

    @Test
    fun invert() {
        assertThat((!isFalse)(request), equalTo(true))
        assertThat((!isTrue)(request), equalTo(false))
        assertThat((!isTrue).toString(), equalTo("NOT (org.http4k.core.Request) -> kotlin.Boolean"))
    }

    @Test
    fun and() {
        assertThat((isFalse and isTrue)(request), equalTo(false))
        assertThat((isTrue and isTrue)(request), equalTo(true))
        assertThat((isTrue and isTrue).toString(), equalTo("(org.http4k.core.Request) -> kotlin.Boolean AND (org.http4k.core.Request) -> kotlin.Boolean"))
    }

    @Test
    fun or() {
        assertThat((isFalse or isFalse)(request), equalTo(false))
        assertThat((isFalse or isTrue)(request), equalTo(true))
        assertThat((isTrue or isTrue)(request), equalTo(true))
        assertThat((isTrue or isTrue).toString(), equalTo("(org.http4k.core.Request) -> kotlin.Boolean OR (org.http4k.core.Request) -> kotlin.Boolean"))
    }
}
