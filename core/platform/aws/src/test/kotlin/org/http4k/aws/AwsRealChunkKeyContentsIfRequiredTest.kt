package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ChunkKeyContentsIfRequired
import org.http4k.filter.ClientFilters
import org.http4k.filter.Payload
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AwsRealChunkKeyContentsIfRequiredTest : AbstractAwsRealS3TestCase() {

    @Test
    fun `default usage`() {
        val requestBodyMode = BodyMode.Memory
        bucketLifecycle(ClientFilters.ChunkKeyContentsIfRequired(requestBodyMode = requestBodyMode)
            .then(awsClientFilter(Payload.Mode.Signed))
            .then(ApacheClient(requestBodyMode = requestBodyMode)))
    }

    @Test
    @Disabled
    fun `streaming usage`() {
        val requestBodyMode = BodyMode.Stream
        bucketLifecycle(ClientFilters.ChunkKeyContentsIfRequired(requestBodyMode = requestBodyMode)
            .then(awsClientFilter(Payload.Mode.Unsigned))
            .then(ApacheClient(requestBodyMode = requestBodyMode)))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val aClient = aClient()

        val contentOriginal = (1..10 * 1024 * 1024).map { 'a' }.joinToString("")

        assertThat(
            "Bucket should not exist in root listing",
            aClient(Request(GET, s3Root)).bodyString(),
            !containsSubstring(bucketName))
        assertThat(
            "Put of bucket should succeed",
            aClient(Request(PUT, bucketUrl)).status,
            equalTo(OK))
        assertThat(
            "Bucket should exist in root listing",
            aClient(Request(GET, s3Root)).bodyString(),
            containsSubstring(bucketName))
        assertThat(
            "Key should not exist in bucket listing",
            aClient(Request(GET, bucketUrl)).bodyString(),
            !containsSubstring(key))

        client(Request(PUT, keyUrl)
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong()))

        assertThat(
            "Key should appear in bucket listing",
            aClient(Request(GET, bucketUrl)).bodyString(),
            containsSubstring(key))
        assertThat(
            "Key contents should be as expected",
            aClient(Request(GET, keyUrl)).bodyString().length,
            equalTo(contentOriginal.length))
        assertThat(
            "Delete of key should succeed",
            aClient(Request(DELETE, keyUrl)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            aClient(Request(GET, bucketUrl)).bodyString(),
            !containsSubstring(key))
        assertThat(
            "Delete of bucket should succeed",
            aClient(Request(DELETE, bucketUrl)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            aClient(Request(GET, s3Root)).bodyString(),
            !containsSubstring(bucketName))
    }
}
