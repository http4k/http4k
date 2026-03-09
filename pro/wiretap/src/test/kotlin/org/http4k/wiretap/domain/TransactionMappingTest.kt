/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.wiretap.util.formatBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class TransactionMappingTest {

    @Test
    fun `prettifies JSON body`(approver: Approver) {
        approver.assertApproved(formatBody("""{"a":1,"b":2}""", "application/json"))
    }

    @Test
    fun `prettifies XML body`(approver: Approver) {
        approver.assertApproved(formatBody("<root><child>text</child></root>", "application/xml"))
    }

    @Test
    fun `prettifies HTML body`(approver: Approver) {
        approver.assertApproved(formatBody("<html><body><p>hello</p></body></html>", "text/html"))
    }

    @Test
    fun `returns raw body for plain text`() {
        assertThat(
            formatBody("just plain text", "text/plain"),
            equalTo("just plain text")
        )
    }

    @Test
    fun `returns raw body when content type is empty`() {
        assertThat(
            formatBody("no content type", ""),
            equalTo("no content type")
        )
    }

    @Test
    fun `returns raw body for invalid JSON`() {
        assertThat(
            formatBody("not json", "application/json"),
            equalTo("not json")
        )
    }

    @Test
    fun `returns raw body for invalid XML`() {
        assertThat(
            formatBody("not xml", "application/xml"),
            equalTo("not xml")
        )
    }

    @Test
    fun `returns blank body unchanged`() {
        assertThat(
            formatBody("", "application/json"),
            equalTo("")
        )
    }

    @Test
    fun `handles json subtypes`(approver: Approver) {
        approver.assertApproved(formatBody("""{"a":1}""", "application/ld+json"))
    }

    @Test
    fun `handles xml subtypes`(approver: Approver) {
        approver.assertApproved(formatBody("<root><child/></root>", "application/soap+xml"))
    }

    @Test
    fun `SSE body normalizes line endings`() {
        assertThat(
            formatBody("data: hello\r\ndata: world\r\n\r\ndata: foo\r", "text/event-stream"),
            equalTo("data: hello\ndata: world\n\ndata: foo\n")
        )
    }

    @Test
    fun `JSON output strips carriage returns`() {
        val result = formatBody("{\"a\":1,\"b\":2}", "application/json")
        assertThat(result.contains("\r"), equalTo(false))
    }

    @Test
    fun `XML output strips carriage returns`() {
        val result = formatBody("<root><child>text</child></root>", "application/xml")
        assertThat(result.contains("\r"), equalTo(false))
    }

    @Test
    fun `binary content type returns stream marker`() {
        assertThat(formatBody("binary data", "image/png"), equalTo("<<stream>>"))
    }

    @Test
    fun `octet-stream returns stream marker`() {
        assertThat(formatBody("binary data", "application/octet-stream"), equalTo("<<stream>>"))
    }

    @Test
    fun `pdf returns stream marker`() {
        assertThat(formatBody("binary data", "application/pdf"), equalTo("<<stream>>"))
    }
}
