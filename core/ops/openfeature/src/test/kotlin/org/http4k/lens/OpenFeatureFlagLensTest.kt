package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import dev.openfeature.sdk.ImmutableContext
import org.http4k.connect.openfeature.FakeOpenFeature
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.PopulateOpenFeatureContext
import org.http4k.filter.ServerFilters
import org.junit.jupiter.api.Test

class OpenFeatureFlagLensTest {

    private val fake = FakeOpenFeature()

    private fun handler(read: (Request) -> String) =
        ServerFilters.PopulateOpenFeatureContext(fake.client()) { ImmutableContext("alice") }
            .then { req -> Response(OK).body(read(req)) }

    @Test
    fun `boolean defaulted returns flag value when present`() {
        fake[FlagKey.of("dark-mode")] = true
        val darkMode = OpenFeatureFlag.boolean().defaulted("dark-mode", false)

        val response = handler { darkMode(it).toString() }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("true"))
    }

    @Test
    fun `boolean defaulted falls back when flag is missing`() {
        val darkMode = OpenFeatureFlag.boolean().defaulted("nope", true)

        val response = handler { darkMode(it).toString() }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("true"))
    }

    @Test
    fun `string optional returns flag value when present`() {
        fake[FlagKey.of("greeting")] = "hello"
        val greeting = OpenFeatureFlag.string().optional("greeting")

        val response = handler { greeting(it) ?: "missing" }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("hello"))
    }

    @Test
    fun `string optional returns null when flag is missing`() {
        val greeting = OpenFeatureFlag.string().optional("nope")

        val response = handler { greeting(it) ?: "missing" }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("missing"))
    }

    @Test
    fun `int required returns value when flag is present`() {
        fake[FlagKey.of("max-items")] = 7
        val maxItems = OpenFeatureFlag.int().required("max-items")

        val response = handler { maxItems(it).toString() }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("7"))
    }

    @Test
    fun `int required throws LensFailure when flag is missing`() {
        val maxItems = OpenFeatureFlag.int().required("nope")

        assertThat({ handler { maxItems(it).toString() }(Request(GET, "/")) }, throws<LensFailure>())
    }

    @Test
    fun `long lens maps from underlying number`() {
        fake[FlagKey.of("count")] = 42
        val count = OpenFeatureFlag.long().defaulted("count", 0L)

        val response = handler { count(it).toString() }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("42"))
    }

    @Test
    fun `lens uses the EvaluationContext populated by the filter`() {
        fake[FlagKey.of("dark-mode")] = false
        fake.rule(FlagKey.of("dark-mode")) { ctx -> ctx.context["targetingKey"] == "alice" } returns true
        val darkMode = OpenFeatureFlag.boolean().defaulted("dark-mode", false)

        val response = handler { darkMode(it).toString() }(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("true"))
    }
}
