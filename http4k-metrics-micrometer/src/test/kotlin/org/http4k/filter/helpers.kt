package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Timer
import java.util.concurrent.TimeUnit

internal fun assert(registry: MeterRegistry, vararg matcher: Matcher<MeterRegistry>) =
    matcher.forEach { assertThat(registry, it) }

internal fun hasCounter(name: String, tags: List<Tag>, matcher: Matcher<Counter>? = null): Matcher<MeterRegistry> =
    has(
        "a counter named $name with tags ${tags.map { "${it.key}=${it.value}" }}",
        {
            it.find(name).tags(tags).counter()
        },
        present(matcher)
    )

internal fun hasTimer(name: String, tags: List<Tag>, matcher: Matcher<Timer>? = null): Matcher<MeterRegistry> =
    has(
        "a timer named $name with tags ${tags.map { "${it.key}=${it.value}" }}",
        { it.find(name).tags(tags).timer() },
        present(matcher)
    )

internal fun counterCount(value: Long) = has<Counter, Long>("count", { it.count().toLong() }, equalTo(value))
internal fun timerCount(value: Long) = has<Timer, Long>("count", { it.count() }, equalTo(value))
internal fun timerTotalTime(millis: Long) =
    has<Timer, Long>("total time", { it.totalTime(TimeUnit.MILLISECONDS).toLong() }, equalTo(millis))

internal fun description(value: String) = has<Meter, String>(
    "description",
    {
        it.id.description ?: "unknown"
    },
    equalTo(value)
)
