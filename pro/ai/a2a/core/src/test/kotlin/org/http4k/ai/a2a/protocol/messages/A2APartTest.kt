/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.model.MimeType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.http4k.format.wrap
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class A2APartTest {

    @Test
    fun `Part with all fields roundtrips correctly`(approver: Approver) {
        val part = A2APart(
            text = "Hello",
            raw = Base64Blob.encode("file content"),
            url = Uri.of("https://example.com/file.txt"),
            data = MoshiNode.wrap(mapOf("key" to "value")),
            metadata = mapOf("source" to "test"),
            filename = "test.txt",
            mediaType = MimeType.of("text/plain")
        )
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APart>(json), equalTo(part))
    }

    @Test
    fun `Part with no fields roundtrips correctly`(approver: Approver) {
        val part = A2APart()
        val json = A2AJson.asFormatString(part)
        approver.assertApproved(json, APPLICATION_JSON)
        assertThat(A2AJson.asA<A2APart>(json), equalTo(part))
    }
}
