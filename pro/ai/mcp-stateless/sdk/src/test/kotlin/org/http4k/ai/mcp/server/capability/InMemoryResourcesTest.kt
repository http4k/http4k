/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Resource
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.junit.jupiter.api.Test

class InMemoryResourcesTest {

    @Test
    fun `can invoke resource by uri with a ResourceRequest`() {
        val expected = ResourceResponse.Ok(Resource.Content.Text("hello", Uri.of("test://resource")))
        val allResources = resources(Resource.Static("test://resource", "my-resource") bind { expected })

        val response = allResources(ResourceRequest(Uri.of("test://resource")))

        assertThat(response, equalTo(expected))
    }
}
