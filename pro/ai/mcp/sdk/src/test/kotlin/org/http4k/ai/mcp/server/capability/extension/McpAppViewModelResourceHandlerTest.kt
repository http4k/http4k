/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability.extension

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps.MIME_TYPE
import org.http4k.core.Uri
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test


class McpAppViewModelResourceHandlerTest {

    object Foo : ViewModel

    @Test
    fun `can create an resource renderer with a template renderer`() {
        val uiUri = Uri.of("ui://foobar")
        val handler = McpAppViewModelResourceHandler(uiUri, { "hello" }) { Foo }

        assertThat(
            handler(ResourceRequest(uiUri)),
            equalTo(ResourceResponse.Ok(Resource.Content.Text("hello", uiUri, MIME_TYPE,
                Content.Meta(ui = McpAppResourceMeta(prefersBorder = null))
            )))
        )
    }
}
