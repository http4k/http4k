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
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.filter.ChunkKeyContentsIfRequired
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload
import org.junit.Test

class AwsRealChunkKeyContentsIfRequiredTest : AbstractAwsRealS3TestCase() {

    @Test
    fun `default usage`() {
        val client =
            ClientFilters.ChunkKeyContentsIfRequired()
                .then(awsClientFilter(Payload.Mode.Signed))
                .then(DebuggingFilters.PrintResponse())
                .then(ApacheClient(requestBodyMode = BodyMode.Request.Memory))
        bucketLifecycle((client))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val aClient = aClient()

        val contentOriginal = (1..10 * 1024 * 1024).map { 'a' }.joinToString("")

        assertThat(
            "Bucket should not exist in root listing",
            aClient(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
        assertThat(
            "Put of bucket should succeed",
            aClient(Request(PUT, bucketUrl!!)).status,
            equalTo(Status.OK))
        assertThat(
            "Bucket should exist in root listing",
            aClient(Request(GET, s3Root!!)).bodyString(),
            containsSubstring(bucketName!!))
        assertThat(
            "Key should not exist in bucket listing",
            aClient(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))

        client(Request(PUT, keyUrl!!)
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong()))

        assertThat(
            "Key should appear in bucket listing",
            aClient(Request(GET, bucketUrl!!)).bodyString(),
            containsSubstring(key!!))
        assertThat(
            "Key contents should be as expected",
            aClient(Request(GET, keyUrl!!)).bodyString().length,
            equalTo(contentOriginal.length))
        assertThat(
            "Delete of key should succeed",
            aClient(Request(DELETE, keyUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            aClient(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))
        assertThat(
            "Delete of bucket should succeed",
            aClient(Request(DELETE, bucketUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            aClient(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
    }
}
