/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class PrettifyTest {

    @Test
    fun `json body is prettified`(approver: Approver) {
        approver.assertApproved(formatBody("""{"a":1,"b":"hello"}""", "application/json"))
    }

    @Test
    fun `invalid json returns original body unchanged`(approver: Approver) {
        approver.assertApproved(formatBody("not json {", "application/json"))
    }

    @Test
    fun `html body is reformatted`(approver: Approver) {
        approver.assertApproved(formatBody("<html><body><p>hi</p></body></html>", "text/html"))
    }

    @Test
    fun `xml body gets indentation`(approver: Approver) {
        approver.assertApproved(formatBody("<root><child>val</child></root>", "application/xml"))
    }

    @Test
    fun `event-stream content passes through unchanged`(approver: Approver) {
        approver.assertApproved(formatBody("data: hello\n\n", "text/event-stream"))
    }

    @Test
    fun `blank body remains blank for json`(approver: Approver) {
        approver.assertApproved(formatBody("", "application/json"))
    }

    @Test
    fun `whitespace body remains unchanged`(approver: Approver) {
        approver.assertApproved(formatBody("  ", "text/html"))
    }

    @Test
    fun `unknown content-type returns stream marker`(approver: Approver) {
        approver.assertApproved(formatBody("binary data", "application/octet-stream"))
    }

    @Test
    fun `text content-type passes through unchanged`(approver: Approver) {
        approver.assertApproved(formatBody("plain text", "text/plain"))
    }

    @Test
    fun `blank content-type returns body unchanged`(approver: Approver) {
        approver.assertApproved(formatBody("some content", ""))
    }

    @Test
    fun `content-type matching is case insensitive`(approver: Approver) {
        approver.assertApproved(formatBody("""{"a":1}""", "APPLICATION/JSON"))
    }
}
