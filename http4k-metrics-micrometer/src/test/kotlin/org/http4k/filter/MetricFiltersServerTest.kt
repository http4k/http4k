package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.should.shouldMatch
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MockClock
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleConfig
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Path
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit

class MetricFiltersServerTest {

    private var config = MetricFilters.Server.Config()
    private val clock = MockClock()
    private val registry by lazy { SimpleMeterRegistry(SimpleConfig.DEFAULT, clock) }
    private val server by lazy {
        MetricFilters.Server(registry, config)
                .then(ServerFilters.CatchAll())
                .then(ServerFilters.CatchLensFailure)
                .then(SimulateResponseTime(clock))
                .then(routes(
                        "/timed" bind routes(
                                "/one" bind Method.GET to { Response(Status.OK) },
                                "/two/{name:.*}" bind Method.POST to { Response(Status.OK).body(Path.of("name")(it)) },
                                "/three" bind Method.GET to { Response(Status.OK) } with counter
                        ) with timer,
                        "/counted" bind routes(
                                "/one" bind Method.GET to { Response(Status.OK) },
                                "/two/{name:.*}" bind Method.POST to { Response(Status.OK).body(Path.of("name")(it)) },
                                "/three" bind Method.GET to { Response(Status.OK) } with timer
                        ) with counter,
                        "/unmetered" bind routes(
                                "one" bind Method.GET to { Response(Status.OK) },
                                "two" bind Method.DELETE to { Response(Status.INTERNAL_SERVER_ERROR) }
                        )
                ))
    }

    @Test
    fun `routes with timer generate handler timing metrics tagged with path and method and status`() {
        server(Request(Method.GET, "/timed/one")) shouldMatch hasStatus(Status.OK)
        repeat(2) {
            server(Request(Method.POST, "/timed/two/bob")) shouldMatch (hasStatus(Status.OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 1),
                hasRequestCounter(Method.POST, Status.OK, 2),
                hasHandlerTimer(Method.GET, "timed_one", Status.OK, 1, Duration.ofMillis(10)),
                hasHandlerTimer(Method.POST, "timed_two_name", Status.OK, 2, Duration.ofMillis(20))
        )
    }

    @Test
    fun `routes with counter generate handler count metrics tagged with path and method and status`() {
        server(Request(Method.GET, "/counted/one")) shouldMatch hasStatus(Status.OK)
        repeat(2) {
            server(Request(Method.POST, "/counted/two/bob")) shouldMatch (hasStatus(Status.OK) and hasBody("bob"))
        }

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 1),
                hasRequestCounter(Method.POST, Status.OK, 2),
                hasHandlerCounter(Method.GET, "counted_one", Status.OK, 1),
                hasHandlerCounter(Method.POST, "counted_two_name", Status.OK, 2)
        )
    }

    @Test
    fun `routes with timer can be overridden with counter`() {
        server(Request(Method.GET, "/timed/three")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 1),
                hasHandlerCounter(Method.GET, "timed_three", Status.OK, 1)
        )
    }

    @Test
    fun `routes with counter can be overridden with timer`() {
        server(Request(Method.GET, "/counted/three")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 1),
                hasHandlerTimer(Method.GET, "counted_three", Status.OK, 1, Duration.ofMillis(10))
        )
    }

    @Test
    fun `routes without metrics generate request count metrics with method and status`() {
        server(Request(Method.GET, "/unmetered/one")) shouldMatch hasStatus(Status.OK)
        server(Request(Method.DELETE, "/unmetered/two")) shouldMatch hasStatus(Status.INTERNAL_SERVER_ERROR)

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 1),
                hasRequestCounter(Method.DELETE, Status.INTERNAL_SERVER_ERROR, 1),
                hasNoHandlerTimer(Method.GET, "unmetered_one", Status.OK),
                hasNoHandlerCounter(Method.GET, "unmetered_one", Status.OK)
        )
    }

    @Test
    fun `meter names and path formatter can be configured`() {
        config = MetricFilters.Server.Config(
                MetricFilters.MeterName("custom.serverRequests", "Custom server requests counter"),
                MetricFilters.MeterName("custom.handlerRequests", "Custom handler requests timer"),
                MetricFilters.MeterName("custom.handlerRequests", "Custom handler requests counter"),
                "customMethod", "customStatus", "customPath",
                { MetricFilters.Server.Config.defaultPathFormatter(it).plus("-custom") }
        )

        server(Request(Method.GET, "/timed/one")) shouldMatch hasStatus(Status.OK)
        server(Request(Method.GET, "/counted/one")) shouldMatch hasStatus(Status.OK)

        assert(registry,
                hasRequestCounter(Method.GET, Status.OK, 2),
                hasHandlerTimer(Method.GET, "timed_one-custom", Status.OK, 1, Duration.ofMillis(10)),
                hasHandlerCounter(Method.GET, "counted_one-custom", Status.OK, 1)
        )
    }

    private fun assert(registry: MeterRegistry, vararg matcher: Matcher<MeterRegistry>) =
            matcher.forEach { assertThat(registry, it) }

    private fun hasRequestCounter(method: Method, status: Status, count: Long) = hasCounter(config.httpServerRequestsCounter.name,
            Tags.zip(config.methodName, method.name, config.statusName, status.code.toString()),
            description(config.httpServerRequestsCounter.description!!) and counterCount(count)
    )

    private fun hasHandlerTimer(method: Method, path: String, status: Status,
                                count: Long, totalTime: Duration) = hasTimer(config.httpServerHandlersTimer.name,
            Tags.zip(config.pathName, path, config.methodName, method.name, config.statusName, status.code.toString()),
            description(config.httpServerHandlersTimer.description!!) and timerCount(count) and timerTotalTime(totalTime.toMillis())
    )

    private fun hasHandlerCounter(method: Method, path: String, status: Status,
                                  count: Long) = hasCounter(config.httpServerHandlersCounter.name,
            Tags.zip(config.pathName, path, config.methodName, method.name, config.statusName, status.code.toString()),
            description(config.httpServerHandlersCounter.description!!) and counterCount(count)
    )

    private fun hasNoHandlerTimer(method: Method, path: String, status: Status) =
            hasTimer(config.httpServerHandlersTimer.name,
                    Tags.zip(config.pathName, path, config.methodName, method.name, config.statusName, status.code.toString())
            ).not()

    private fun hasNoHandlerCounter(method: Method, path: String, status: Status) =
            hasCounter(config.httpServerHandlersCounter.name,
                    Tags.zip(config.pathName, path, config.methodName, method.name, config.statusName, status.code.toString())
            ).not()

    private fun hasCounter(name: String, tags: List<Tag>, matcher: Matcher<Counter>? = null): Matcher<MeterRegistry> =
            has("a counter named $name with tags ${tags.map { "${it.key}=${it.value}" }}",
                    { it.find(name).tags(tags).counter().orNull() },
                    present(matcher)
            )

    private fun hasTimer(name: String, tags: List<Tag>, matcher: Matcher<Timer>? = null): Matcher<MeterRegistry> =
            has("a timer named $name with tags ${tags.map { "${it.key}=${it.value}" }}",
                    { it.find(name).tags(tags).timer().orNull() },
                    present(matcher)
            )

    private fun counterCount(value: Long) = has<Counter, Long>("count", { it.count().toLong() }, equalTo(value))
    private fun timerCount(value: Long) = has<Timer, Long>("count", { it.count() }, equalTo(value))
    private fun timerTotalTime(millis: Long) =
            has<Timer, Long>("total time", { it.totalTime(TimeUnit.MILLISECONDS).toLong() }, equalTo(millis))
    private fun description(value: String) = has<Meter, String>("description", { it.id.description }, equalTo(value))

    private fun <T : Meter> Optional<T>.orNull(): T? = orElse(null)
}

private object SimulateResponseTime {
    operator fun invoke(clock: MockClock): Filter = Filter { next: HttpHandler ->
        {
            try {
                next(it)
            } finally {
                clock.add(10, TimeUnit.MILLISECONDS)
            }
        }
    }
}