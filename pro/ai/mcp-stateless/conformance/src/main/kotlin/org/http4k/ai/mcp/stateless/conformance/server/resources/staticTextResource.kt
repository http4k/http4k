/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.resources

import org.http4k.ai.mcp.stateless.ResourceResponse
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.ResourceName
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.routing.stateless.bind

fun staticTextResource() =
    Resource.Static(Uri.of("test://static-text"), ResourceName.of("static-text"), null, MimeType.TEXT_PLAIN) bind {
        ResourceResponse.Ok(
            listOf(
                Resource.Content.Text(
                    "This is the content of the static text resource.",
                    Uri.of("test://static-text"),
                    MimeType.TEXT_PLAIN
                )
            )
        )
    }
