package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Value
import org.http4k.connect.openfeature.FakeOpenFeature
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.jupiter.api.Test

class PopulateOpenFeatureFlagContextTest {

    private val fake = FakeOpenFeature()

    @Test
    fun `populates the snapshot context with the extractor result`() {
        val handler = ServerFilters.PopulateOpenFeatureContext(fake.client()) { ImmutableContext("alice") }
            .then { req -> Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).context.targetingKey) }

        val response = handler(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("alice"))
    }

    @Test
    fun `extractor sees the incoming request`() {
        val handler = ServerFilters.PopulateOpenFeatureContext(fake.client()) { req ->
            ImmutableContext(req.header("X-User") ?: "")
        }.then { req -> Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).context.targetingKey) }

        assertThat(handler(Request(GET, "/").header("X-User", "bob")).bodyString(), equalTo("bob"))
        assertThat(handler(Request(GET, "/")).bodyString(), equalTo(""))
    }

    @Test
    fun `populates non-user attributes when there is no targeting key`() {
        val handler = ServerFilters.PopulateOpenFeatureContext(fake.client()) {
            ImmutableContext("", mapOf("locale" to Value("en-GB")))
        }.then { req ->
            Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).context.getValue("locale").asString())
        }

        assertThat(handler(Request(GET, "/")).bodyString(), equalTo("en-GB"))
    }

    @Test
    fun `attaches the bulk-evaluated flags to the snapshot`() {
        fake[FlagKey.of("dark-mode")] = true
        fake[FlagKey.of("greeting")] = "hello"

        val handler = ServerFilters.PopulateOpenFeatureContext(fake.client()) { ImmutableContext("alice") }
            .then { req ->
                val snapshot = OPENFEATURE_CONTEXT_KEY(req)
                val darkMode = snapshot.flags[FlagKey.of("dark-mode")]?.value
                val greeting = snapshot.flags[FlagKey.of("greeting")]?.value
                Response(OK).body("$darkMode/$greeting")
            }

        assertThat(handler(Request(GET, "/")).bodyString(), equalTo("true/hello"))
    }
}
