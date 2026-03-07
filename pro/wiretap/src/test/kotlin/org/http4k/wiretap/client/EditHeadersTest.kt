package org.http4k.wiretap.client

import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.template.DatastarElementRenderer
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.wiretap.util.Json.json
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class EditHeadersTest {

    private val renderer = DatastarElementRenderer(Templates())
    private val handler = EditHeaders(renderer, "/_wiretap/inbound/", "/")

    @Test
    fun `add header increments index`(approver: Approver) {
        approver.assertApproved(
            handler(
                Request(POST, "/headers/add")
                    .json(ClientRequest(headers = mapOf("0" to HeaderEntry("Accept", "text/html"))))
            )
        )
    }

    @Test
    fun `add header to empty starts at index 0`(approver: Approver) {
        approver.assertApproved(
            handler(
                Request(POST, "/headers/add")
                    .json(ClientRequest(headers = emptyMap()))
            )
        )
    }

    @Test
    fun `remove header updates rendered output`(approver: Approver) {
        approver.assertApproved(
            handler(
                Request(POST, "/headers/remove/0")
                    .json(
                        ClientRequest(
                            headers = mapOf(
                                "0" to HeaderEntry("Accept", "text/html"),
                                "1" to HeaderEntry("Authorization", "Bearer token")
                            )
                        )
                    )
            )
        )
    }

    @Test
    fun `remove last header returns empty response`(approver: Approver) {
        approver.assertApproved(
            handler(
                Request(POST, "/headers/remove/0")
                    .json(ClientRequest(headers = mapOf("0" to HeaderEntry("Accept", "text/html"))))
            )
        )
    }

    @Test
    fun `reset returns to default state`(approver: Approver) {
        approver.assertApproved(
            handler(Request(POST, "/headers/reset"))
        )
    }

}
