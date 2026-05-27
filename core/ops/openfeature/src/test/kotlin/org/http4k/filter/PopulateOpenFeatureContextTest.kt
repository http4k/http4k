package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.openfeature.sdk.ImmutableContext
import dev.openfeature.sdk.Value
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.junit.jupiter.api.Test

class PopulateOpenFeatureContextTest {

    @Test
    fun `populates the shared key with the extractor result`() {
        val handler = ServerFilters.PopulateOpenFeatureContext { ImmutableContext("alice") }
            .then { req -> Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).targetingKey) }

        val response = handler(Request(GET, "/"))

        assertThat(response.bodyString(), equalTo("alice"))
    }

    @Test
    fun `extractor sees the incoming request`() {
        val handler = ServerFilters.PopulateOpenFeatureContext { req ->
            ImmutableContext(req.header("X-User") ?: "")
        }.then { req -> Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).targetingKey) }

        assertThat(handler(Request(GET, "/").header("X-User", "bob")).bodyString(), equalTo("bob"))
        assertThat(handler(Request(GET, "/")).bodyString(), equalTo(""))
    }

    @Test
    fun `populates non-user attributes when there is no targeting key`() {
        val handler = ServerFilters.PopulateOpenFeatureContext {
            ImmutableContext("", mapOf("locale" to Value("en-GB")))
        }.then { req ->
            Response(OK).body(OPENFEATURE_CONTEXT_KEY(req).getValue("locale").asString())
        }

        assertThat(handler(Request(GET, "/")).bodyString(), equalTo("en-GB"))
    }
}
