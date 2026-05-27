package org.http4k.ops.openfeature

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.openfeature.sdk.ErrorCode
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Reason
import dev.openfeature.sdk.Value
import org.http4k.connect.openfeature.FakeOpenFeature
import org.http4k.connect.openfeature.model.FlagKey
import org.junit.jupiter.api.Test

class Http4KOpenFeatureFlagProviderTest {

    private val fake = FakeOpenFeature()
    private val provider = Http4kOpenFeatureProvider(fake.client())
    private val ctx = ImmutableContext("user-123")

    @Test
    fun `boolean flag returns the seeded value`() {
        fake.set(FlagKey.of("dark-mode"), true)

        val result = provider.getBooleanEvaluation("dark-mode", false, ctx)

        assertThat(result.value, equalTo(true))
        assertThat(result.errorCode, absent())
        assertThat(result.reason, equalTo(Reason.STATIC.name))
    }

    @Test
    fun `string flag returns the seeded value`() {
        fake.set(FlagKey.of("greeting"), "hello")

        val result = provider.getStringEvaluation("greeting", "default", ctx)

        assertThat(result.value, equalTo("hello"))
    }

    @Test
    fun `integer flag coerces from JSON number`() {
        fake.set(FlagKey.of("max-attempts"), 5)

        val result = provider.getIntegerEvaluation("max-attempts", 0, ctx)

        assertThat(result.value, equalTo(5))
    }

    @Test
    fun `double flag coerces from JSON number`() {
        fake.set(FlagKey.of("rate"), 0.25)

        val result = provider.getDoubleEvaluation("rate", 0.0, ctx)

        assertThat(result.value, equalTo(0.25))
    }

    @Test
    fun `missing flag returns default with FLAG_NOT_FOUND error code`() {
        val result = provider.getBooleanEvaluation("missing", true, ctx)

        assertThat(result.value, equalTo(true))
        assertThat(result.errorCode, equalTo(ErrorCode.FLAG_NOT_FOUND))
        assertThat(result.reason, equalTo(Reason.ERROR.name))
    }

    @Test
    fun `type mismatch returns default with TYPE_MISMATCH error code`() {
        fake.set(FlagKey.of("dark-mode"), "not-a-boolean")

        val result = provider.getBooleanEvaluation("dark-mode", false, ctx)

        assertThat(result.value, equalTo(false))
        assertThat(result.errorCode, equalTo(ErrorCode.TYPE_MISMATCH))
    }

    @Test
    fun `object flag wraps value`() {
        fake.set(FlagKey.of("config"), mapOf("max" to 10, "min" to 1))

        val result = provider.getObjectEvaluation("config", Value(), ctx)

        assertThat(result.errorCode, absent())
    }
}
