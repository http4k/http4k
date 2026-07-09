package org.http4k.contract.openapi.v3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.Root
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiLicense
import org.http4k.contract.openapi.OpenApiVersion._2_0_0
import org.http4k.contract.openapi.OpenApiVersion._3_1_0
import org.http4k.contract.openapi.OpenApiVersion._3_2_0
import org.http4k.core.Response
import org.http4k.format.Argo
import org.http4k.security.NoSecurity
import org.junit.jupiter.api.Test

class OpenApi3JsonSchemaDialectTest {
    @Test
    fun `renders the json schema dialect matching the configured version`() {
        val renderer = OpenApi3(
            ApiInfo("title", "1.2", "description", "summary", ApiLicense.Apache2_0),
            Argo,
            version = _3_2_0
        )

        assertThat(
            renderer.render().bodyString(),
            containsSubstring(""""jsonSchemaDialect":"${_3_2_0.jsonSchemaDialect}"""")
        )
    }

    @Test
    fun `omits the json schema dialect for versions that don't declare one`() {
        val renderer = OpenApi3(
            ApiInfo("title", "1.2", "description", "summary", ApiLicense.Apache2_0),
            Argo,
            version = _2_0_0
        )

        assertThat(renderer.render().bodyString().contains("jsonSchemaDialect"), equalTo(false))
    }

    @Test
    fun `switching the configured version changes the rendered dialect`() {
        val renderer = OpenApi3(
            ApiInfo("title", "1.2", "description", "summary", ApiLicense.Apache2_0),
            Argo,
            version = _3_1_0
        )

        assertThat(
            renderer.render().bodyString(),
            containsSubstring(""""jsonSchemaDialect":"${_3_1_0.jsonSchemaDialect}"""")
        )
    }

    private fun OpenApi3<*>.render(): Response =
        description(Root, NoSecurity, emptyList(), emptySet(), emptyMap())
}
