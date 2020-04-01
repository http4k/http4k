package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.filter.Payload
import org.junit.jupiter.api.Test

class AwsRealMultipartTest : AbstractAwsRealS3TestCase() {

    @Test
    fun `default usage`() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(ApacheClient(requestBodyMode = BodyMode.Memory))
        bucketLifecycle((client))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val contentOriginal = (1..5 * 1024 * 1024).map { 'a' }.joinToString("")

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

        /* initialise multipart */
        val initialiseUpload = client(Request(POST, keyUrl.query("uploads", "")))
        assertThat("Initialise of key should succeed", initialiseUpload.status, equalTo(OK))
        val uploadId = UploadId.from(initialiseUpload)

        /* upload a part */
        val firstPart = client(Request(PUT, keyUrl
            .query("partNumber", "1")
            .query("uploadId", uploadId.value))
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong())
        )
        assertThat("First part upload", firstPart.status, equalTo(OK))
        val etag1 = firstPart.header("ETag")!!

        /* upload another part */
        val secondPart = client(Request(PUT, keyUrl
            .query("partNumber", "2")
            .query("uploadId", uploadId.value))
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong())
        )
        assertThat("Second part upload", secondPart.status, equalTo(OK))
        val etag2 = secondPart.header("ETag")!!

        /* finalise multipart */
        val finaliseUpload = client(Request(POST, keyUrl.query("uploadId", uploadId.value))
            .body(listOf(etag1, etag2).asSequence().toCompleteMultipartUploadXml()))
        assertThat("Finalize of key should succeed", finaliseUpload.status, equalTo(OK))

        assertThat(
            "Key should appear in bucket listing",
            client(Request(GET, bucketUrl)).bodyString(),
            containsSubstring(key))
        assertThat(
            "Key contents should be as expected",
            client(Request(GET, keyUrl)).bodyString(),
            equalTo(contentOriginal + contentOriginal))
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
