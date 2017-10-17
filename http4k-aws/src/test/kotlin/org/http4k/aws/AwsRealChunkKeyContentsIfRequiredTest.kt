package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload
import org.junit.Test

class AwsRealChunkKeyContentsIfRequiredTest : AbstractAwsRealS3TestCase() {

    @Test
    fun `default usage`() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(ClientFilters.ChunkKeyContentsIfRequired())
            .then(DebuggingFilters.PrintRequestAndResponse())
            .then(ApacheClient(requestBodyMode = BodyMode.Request.Memory))
        bucketLifecycle((client))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val contentOriginal = (1..10 * 1024 * 1024).map { 'a' }.joinToString("")

        assertThat(
            "Bucket should not exist in root listing",
            client(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
        assertThat(
            "Put of bucket should succeed",
            client(Request(PUT, bucketUrl!!)).status,
            equalTo(Status.OK))
        assertThat(
            "Bucket should exist in root listing",
            client(Request(GET, s3Root!!)).bodyString(),
            containsSubstring(bucketName!!))
        assertThat(
            "Key should not exist in bucket listing",
            client(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))

        client(Request(PUT, keyUrl!!)
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong()))

        assertThat(
            "Key should appear in bucket listing",
            client(Request(GET, bucketUrl!!)).bodyString(),
            containsSubstring(key!!))
        assertThat(
            "Key contents should be as expected",
            client(Request(GET, keyUrl!!)).bodyString(),
            equalTo(contentOriginal))
        assertThat(
            "Delete of key should succeed",
            client(Request(DELETE, keyUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            client(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))
        assertThat(
            "Delete of bucket should succeed",
            client(Request(DELETE, bucketUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            client(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
    }

}

fun ClientFilters.ChunkKeyContentsIfRequired(): Filter = Filter { next ->
    {
        if (it.method == PUT && it.uri.path == "") upload.then(next)(it)
        else next(it)
    }
}

private fun Response.onSuccess(fn: (Response) -> Response): Response =
    if (this.status == Status.OK) fn(this) else this

private val upload = Filter { next ->
    {
        next(Request(POST, it.uri.query("uploads", "")))
            .onSuccess { initialiseUpload ->
                val uploadId = UploadId.from(initialiseUpload)

                val partEtags = it.chunks()
                    .withIndex()
                    .map { it.copy(index = it.index + 1) }
                    .mapNotNull { (index, part) ->
                        val upload = next(Request(PUT, it.uri
                            .query("partNumber", index.toString())
                            .query("uploadId", uploadId))
                            .body(part)
                        )
                        // todo: stop on failure
                        upload.header("ETag")
                    }

                next(Request(POST, it.uri.query("uploadId", uploadId))
                    .body(partEtags.toCompleteMultipartUploadXml()))
            }
    }
}

private fun Request.chunks() =
    listOf(
        bodyString().substring((0..5000)),
        bodyString().substring((5000..10000))
    ).asSequence()

private data class UploadId(val value: String) {
    companion object {
        fun from(response: Response) =
            Regex(""".*UploadId>(.+)</UploadId.*""").find(response.bodyString())?.groupValues?.get(1)
    }
}

fun Sequence<String>.toCompleteMultipartUploadXml(): String =
    """<CompleteMultipartUpload>${mapIndexed { index, etag ->
        """<Part><PartNumber>${index + 1}</PartNumber><ETag>$etag</ETag></Part>"""
    }.joinToString("")}</CompleteMultipartUpload>"""
