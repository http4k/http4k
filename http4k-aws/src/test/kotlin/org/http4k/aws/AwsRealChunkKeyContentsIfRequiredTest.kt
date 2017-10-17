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
        val client =
            ClientFilters.ChunkKeyContentsIfRequired()
                .then(awsClientFilter(Payload.Mode.Signed))
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
            client(Request(GET, keyUrl!!)).bodyString().length,
            equalTo(contentOriginal.length))
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

fun ClientFilters.ChunkKeyContentsIfRequired(size: Int = 5 * 1024 * 1024): Filter {
    val chunker = chunker(size)
    return Filter { next ->
        {
            if (it.method == PUT && it.uri.path.trimEnd('/').isNotBlank()) chunker.then(next)(it) else next(it)
        }
    }
}

private fun Response.orFail(uploadId: UploadId? = null): Response = apply { if (this.status != Status.OK) throw UploadError(this, uploadId) }

private data class UploadError(val response: Response, val uploadId: UploadId?) : Exception()

private fun chunker(size: Int) = Filter { next ->
    { request ->
        try {
            val uploadId = UploadId.from(next(Request(POST, request.uri.query("uploads", ""))).orFail())

            val partEtags = request.chunks(size)
                .withIndex()
                .map { it.copy(index = it.index + 1) }
                .mapNotNull { (index, part) ->
                    val upload = next(Request(PUT, request.uri
                        .query("partNumber", index.toString())
                        .query("uploadId", uploadId.value))
                        .body(part)
                    ).orFail(uploadId)
                    upload.header("ETag")
                }

            next(Request(POST, request.uri.query("uploadId", uploadId.value))
                .body(partEtags.toCompleteMultipartUploadXml()))
        } catch (e: UploadError) {
            e.uploadId?.let { next(Request(DELETE, request.uri.query("uploadId", it.value))) }
            e.response
        }
    }
}

private fun Request.chunks(size: Int): Sequence<String> {
    val bodyString = bodyString()
    println(size)
    println(bodyString.length)
    println(size * 2)
    return listOf(
        bodyString.substring((0..size)),
        bodyString.substring((size until (size * 2)))
    ).asSequence()
}

internal data class UploadId(val value: String) {
    companion object {
        fun from(response: Response) =
            Regex(""".*UploadId>(.+)</UploadId.*""").find(response.bodyString())?.groupValues?.get(1)!!.let(::UploadId)
    }
}

internal fun Sequence<String>.toCompleteMultipartUploadXml(): String =
    """<CompleteMultipartUpload>${mapIndexed { index, etag ->
        """<Part><PartNumber>${index + 1}</PartNumber><ETag>$etag</ETag></Part>"""
    }.joinToString("")}</CompleteMultipartUpload>"""
