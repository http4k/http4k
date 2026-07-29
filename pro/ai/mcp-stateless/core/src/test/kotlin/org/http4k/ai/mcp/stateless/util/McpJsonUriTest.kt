/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.util

import org.http4k.ai.mcp.stateless.model.McpUri
import org.http4k.ai.mcp.stateless.protocol.messages.McpResource
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class McpJsonUriTest {

    private val uris = listOf(
        "http://host/path?q=1#frag",
        "https://user@host:8443/",
        "https://host",
        "file:///etc/hosts",
        "file:/no-authority",
        "file://server/share/x",
        "urn:isbn:1",
        "mailto:a@b.c",
        "custom-scheme://thing/with/path",
        "s3://bucket/key.txt",
        "/just/a/path",
        "relative/path",
    )

    @Test
    fun `resource uris are not mangled on the way through a message`(approver: Approver) {
        approver.assertApproved(
            McpJson.asFormatString(uris.map {
                McpJson.asA<McpResource.Read.Request>(
                    McpJson.asFormatString(McpResource.Read.Request(McpResource.Read.Request.Params(McpUri.of(it)), "1"))
                ).params.uri.value
            }),
            APPLICATION_JSON
        )
    }
}
