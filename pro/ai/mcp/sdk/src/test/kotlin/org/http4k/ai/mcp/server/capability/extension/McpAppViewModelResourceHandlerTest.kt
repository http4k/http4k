package org.http4k.ai.mcp.server.capability.extension

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.core.Uri
import org.http4k.template.HandlebarsTemplates
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test

class McpAppViewModelResourceHandlerTest {

    object Foo : ViewModel

    @Test
    fun `can create an resource renderer with a template renderer`() {
        val uiUri = Uri.of("ui://foobar")
        val handler = McpAppViewModelResourceHandler(uiUri, HandlebarsTemplates().CachingClasspath()) { Foo }

        assertThat(
            handler(ResourceRequest(uiUri)),
            equalTo(ResourceResponse.Ok(Resource.Content.Text("", uiUri)))
        )
    }
}
