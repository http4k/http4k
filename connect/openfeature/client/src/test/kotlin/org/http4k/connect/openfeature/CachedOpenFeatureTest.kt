package org.http4k.connect.openfeature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure
import org.http4k.connect.openfeature.action.EvaluateAllFlags
import org.http4k.connect.openfeature.action.EvaluateFlag
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.TargetingKey
import org.http4k.connect.successValue
import org.http4k.util.TickingClock
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

private class CountingOpenFeature(private val delegate: OpenFeature) : OpenFeature {
    val calls = AtomicInteger(0)
    override fun <R : Any> invoke(action: OpenFeatureAction<R>): Result<R, RemoteFailure> {
        calls.incrementAndGet()
        return delegate(action)
    }
}

class CachedOpenFeatureTest {

    private val fake = FakeOpenFeature().also { it[FlagKey.of("dark-mode")] = true }
    private val counting = CountingOpenFeature(fake.client())
    private val clock = TickingClock()
    private val cached = OpenFeature.Cached(counting, ttl = Duration.ofMinutes(5), clock = clock)

    private val key = FlagKey.of("dark-mode")
    private val context = EvaluationContext(TargetingKey.of("alice"))

    @Test
    fun `repeated single-flag evaluations for the same context hit the delegate once`() {
        val first = cached(EvaluateFlag(key, context)).successValue()
        val second = cached(EvaluateFlag(key, context)).successValue()
        val third = cached(EvaluateFlag(key, context)).successValue()

        assertThat(first.value, equalTo<Any?>(true))
        assertThat(second.value, equalTo<Any?>(true))
        assertThat(third.value, equalTo<Any?>(true))
        assertThat(counting.calls.get(), equalTo(1))
    }

    @Test
    fun `single-flag evaluations for different contexts are cached independently`() {
        cached(EvaluateFlag(key, EvaluationContext(TargetingKey.of("alice"))))
        cached(EvaluateFlag(key, EvaluationContext(TargetingKey.of("alice"))))
        cached(EvaluateFlag(key, EvaluationContext(TargetingKey.of("bob"))))

        assertThat(counting.calls.get(), equalTo(2))
    }

    @Test
    fun `single-flag cache expires after ttl`() {
        cached(EvaluateFlag(key, context))
        clock.tick(Duration.ofMinutes(6))
        cached(EvaluateFlag(key, context))

        assertThat(counting.calls.get(), equalTo(2))
    }

    @Test
    fun `repeated bulk evaluations for the same context hit the delegate once`() {
        cached(EvaluateAllFlags(context))
        cached(EvaluateAllFlags(context))
        cached(EvaluateAllFlags(context))

        assertThat(counting.calls.get(), equalTo(1))
    }

    @Test
    fun `bulk evaluations for different contexts are cached independently`() {
        cached(EvaluateAllFlags(EvaluationContext(TargetingKey.of("alice"))))
        cached(EvaluateAllFlags(EvaluationContext(TargetingKey.of("alice"))))
        cached(EvaluateAllFlags(EvaluationContext(TargetingKey.of("bob"))))

        assertThat(counting.calls.get(), equalTo(2))
    }

    @Test
    fun `bulk cache expires after ttl`() {
        cached(EvaluateAllFlags(context))
        clock.tick(Duration.ofMinutes(6))
        cached(EvaluateAllFlags(context))

        assertThat(counting.calls.get(), equalTo(2))
    }

    @Test
    fun `single-flag and bulk caches are independent`() {
        cached(EvaluateFlag(key, context))
        cached(EvaluateAllFlags(context))

        assertThat(counting.calls.get(), equalTo(2))
    }
}
