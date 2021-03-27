package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload
import org.http4k.filter.inIntelliJOnly
import org.junit.jupiter.api.Test
import java.util.UUID

class AwsRealTest : AbstractAwsRealS3TestCase() {

    init {
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true")
    }

    @Test
    fun `default usage`() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(DebuggingFilters.PrintRequestAndResponse().inIntelliJOnly())
            .then(JavaHttpClient())
        bucketLifecycle((client))
    }

    @Test
    fun streaming() {
        val client = awsClientFilter(Payload.Mode.Unsigned)
            .then(JavaHttpClient())

        bucketLifecycle((client))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val contentOriginal = UUID.randomUUID().toString()
        val contents = contentOriginal.byteInputStream()

        assertThat(
            "Bucket should not exist in root listing",
            client(Request(GET, s3Root)).bodyString(),
            !containsSubstring(bucketName))
        assertThat(
            "Put of bucket should succeed",
            client(Request(PUT, bucketUrl)).status,
            equalTo(OK))
        assertThat(
            "Bucket should exist in root listing",
            client(Request(GET, s3Root)).bodyString(),
            containsSubstring(bucketName))
        assertThat(
            "Key should not exist in bucket listing",
            client(Request(GET, bucketUrl)).bodyString(),
            !containsSubstring(key))
        assertThat(
            "Put of key should succeed",
            client(Request(PUT, keyUrl).body(contents, contentOriginal.length.toLong())).status,
            equalTo(OK))
        assertThat(
            "Key should appear in bucket listing",
            client(Request(GET, bucketUrl)).bodyString(),
            containsSubstring(key))
        assertThat(
            "Key contents should be as expected",
            client(Request(GET, keyUrl)).bodyString(),
            equalTo(contentOriginal))
        assertThat(
            "Delete of key should succeed",
            client(Request(DELETE, keyUrl)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            client(Request(GET, bucketUrl)).bodyString(),
            !containsSubstring(key))
        assertThat(
            "Delete of bucket should succeed",
            client(Request(DELETE, bucketUrl)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            client(Request(GET, s3Root)).bodyString(),
            !containsSubstring(bucketName))
    }
}
