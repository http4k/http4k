package org.http4k.chaos

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
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
import org.http4k.core.Status
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
        trigger(request) shouldMatch equalTo(false)
        sleep(200)
        trigger(request) shouldMatch equalTo(true)
    }
}

class CountdownTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"countdown","count":"1"}"""
    override val expectedDescription = "Countdown (1 remaining)"

    @Test
    fun `behaves as expected`() {
        val trigger = Countdown(1)
        trigger(request) shouldMatch equalTo(true)
        trigger(request) shouldMatch equalTo(false)
        trigger(request) shouldMatch equalTo(false)
    }
}

class DelayTriggerTest : ChaosTriggerContract() {
    override val asJson = """{"type":"delay","period":"PT0.1S"}"""
    override val expectedDescription = "Delay (expires 1970-01-01T00:00:00.100Z)"

    @Test
    fun `behaves as expected`() {
        val clock = Clock.systemUTC()
        val trigger = Delay(Duration.ofMillis(100), clock)
        trigger(request) shouldMatch equalTo(false)
        sleep(200)
        trigger(request) shouldMatch equalTo(true)
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
        assertMatchNoMatch(MatchRequest(path = Regex(".*bob")),
                Request(GET, "/abob"),
                Request(GET, "/bill"))
    }

    @Test
    fun `matches header`() {
        assertMatchNoMatch(MatchRequest(headers = mapOf("header" to Regex(".*bob"))),
                request.header("header", "abob"),
                request.header("header", "bill"))
    }

    @Test
    fun `matches query`() {
        assertMatchNoMatch(MatchRequest(queries = mapOf("query" to Regex(".*bob"))),
                request.query("query", "abob"),
                request.query("query", "bill"))
    }

    @Test
    fun `matches body`() {
        assertMatchNoMatch(MatchRequest(body = Regex(".*bob")),
                request.body("abob"),
                request.body("bill"))
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
        trigger(request) shouldMatch equalTo(true)
        trigger.toggle()
        trigger(request) shouldMatch equalTo(false)
        trigger.toggle(false)
        trigger(request) shouldMatch equalTo(false)
        trigger.toggle(true)
        trigger(request) shouldMatch equalTo(true)
    }
}

class AlwaysTest : ChaosTriggerContract() {
    override val asJson = """{"type":"always"}"""

    override val expectedDescription = "Always"

    @Test
    fun `Always applies by default`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Always).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
    }
}

class PercentageBasedTest : ChaosTriggerContract() {
    override val asJson = """{"type":"percentage", "percentage":100}"""

    override val expectedDescription = "PercentageBased (100%)"

    @Test
    fun `from environment`() {
        PercentageBased.fromEnvironment({ Properties().apply { put("CHAOS_PERCENTAGE", "75") }.getProperty(it) }).toString() shouldMatch equalTo("PercentageBased (75%)")
        PercentageBased.fromEnvironment().toString() shouldMatch equalTo("PercentageBased (50%)")
    }

    @Test
    fun `PercentageBased applied`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(PercentageBased(100)).asFilter().then { Response(OK) }
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasBody("")).and(hasHeader("x-http4k-chaos", "Status 500"))
    }
}

class OnceTest : ChaosTriggerContract() {
    override val asJson = """{"type":"once","trigger":{"type":"deadline","endTime":"1970-01-01T00:00:00Z"}}"""

    override val expectedDescription = "Once (trigger = Deadline (1970-01-01T00:00:00Z))"

    @Test
    fun `Once only fires once`() {
        val http = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Once { it.method == GET }).asFilter().then { Response(Status.OK) }
        http(Request(POST, "/foo")) shouldMatch hasStatus(OK)
        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR)
        http(Request(POST, "/foo")) shouldMatch hasStatus(OK)
        http(Request(GET, "/foo")) shouldMatch hasStatus(OK)
    }
}

class ChaosPolicyOperationTest {

    @Test
    fun `Until stops a behaviour when triggered`() {
        val stage: Stage = ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(Always).until { it.method == POST }
        stage.toString() shouldMatch equalTo("Always ReturnStatus (500) until (org.http4k.core.Request) -> kotlin.Boolean")

        val http: HttpHandler = stage.asFilter().then { Response(Status.OK) }

        http(Request(GET, "/foo")) shouldMatch hasStatus(INTERNAL_SERVER_ERROR).and(hasHeader("x-http4k-chaos", "Status 500"))
        http(Request(POST, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
        http(Request(GET, "/bar")) shouldMatch hasStatus(OK).and(!hasHeader("x-http4k-chaos"))
    }
}

class ChaosTriggerLogicalOperatorTest {
    private val isFalse: Trigger = { _: Request -> false }
    private val isTrue: Trigger = { _: Request -> true }

    @Test
    fun invert() {
        (!isFalse)(request) shouldMatch equalTo(true)
        (!isTrue)(request) shouldMatch equalTo(false)
        (!isTrue).toString() shouldMatch equalTo("NOT (org.http4k.core.Request) -> kotlin.Boolean")
    }

    @Test
    fun and() {
        (isFalse and isTrue)(request) shouldMatch equalTo(false)
        (isTrue and isTrue)(request) shouldMatch equalTo(true)
        (isTrue and isTrue).toString() shouldMatch equalTo("(org.http4k.core.Request) -> kotlin.Boolean AND (org.http4k.core.Request) -> kotlin.Boolean")
    }

    @Test
    fun or() {
        (isFalse or isFalse)(request) shouldMatch equalTo(false)
        (isFalse or isTrue)(request) shouldMatch equalTo(true)
        (isTrue or isTrue)(request) shouldMatch equalTo(true)
        (isTrue or isTrue).toString() shouldMatch equalTo("(org.http4k.core.Request) -> kotlin.Boolean OR (org.http4k.core.Request) -> kotlin.Boolean")
    }
}