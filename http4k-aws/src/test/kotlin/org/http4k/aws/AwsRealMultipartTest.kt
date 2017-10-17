package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.Payload
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.InputStream
import java.util.*

class AwsRealMultipartTest {
    private var bucketName: String? = null
    private var key: String? = null
    private var bucketUrl: Uri? = null
    private var keyUrl: Uri? = null
    private var s3Root: Uri? = null
    private var scope: AwsCredentialScope? = null
    private var credentials: AwsCredentials? = null

    @Before
    fun createClient() {
        val properties = Properties()
        properties.load(properties())

        assertThat(
            "Developer should understand what this test does- set signMyLifeAway property to the expected value.",
            properties.getProperty("signMyLifeAway"),
            equalTo("I've checked the code of this test and understand that it creates and deletes buckets and keys using my credentials"))

        scope = AwsCredentialScope(properties.getProperty("region"), properties.getProperty("service"))
        credentials = AwsCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey"))

        bucketName = UUID.randomUUID().toString()
        key = UUID.randomUUID().toString()
        bucketUrl = Uri.of("https://$bucketName.s3.amazonaws.com/")
        keyUrl = Uri.of("https://$bucketName.s3.amazonaws.com/$key")
        s3Root = Uri.of("https://s3.amazonaws.com/")
    }

    private fun awsClientFilter(signed: Payload.Mode) = ClientFilters.AwsAuth(scope!!, credentials!!, payloadMode = signed)

    @Test
    fun `default usage`() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(DebuggingFilters.PrintRequestAndResponse())
            .then(ApacheClient(requestBodyMode = BodyMode.Request.Memory))
        bucketLifecycle((client))
    }

    private fun bucketLifecycle(client: HttpHandler) {
        val contentOriginal = (1..5 * 1024 * 1024).map { 'a' }.joinToString("")

        assertThat(
            "Bucket should not exist in root listing",
            client(Request(Method.GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
        assertThat(
            "Put of bucket should succeed",
            client(Request(Method.PUT, bucketUrl!!)).status,
            equalTo(Status.OK))
        assertThat(
            "Bucket should exist in root listing",
            client(Request(Method.GET, s3Root!!)).bodyString(),
            containsSubstring(bucketName!!))
        assertThat(
            "Key should not exist in bucket listing",
            client(Request(Method.GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))

        /* initialise multipart */
        val initialiseUpload = client(Request(Method.POST, keyUrl!!.query("uploads", "")))
        assertThat("Initialise of key should succeed", initialiseUpload.status, equalTo(Status.OK))
        val uploadId = initialiseUpload.uploadId()

        /* upload a part */
        val firstPart = client(Request(Method.PUT, keyUrl!!
            .query("partNumber", "1")
            .query("uploadId", uploadId))
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong())
        )
        assertThat("First part upload", firstPart.status, equalTo(Status.OK))
        val etag1 = firstPart.header("ETag")!!

        /* upload another part */
        val secondPart = client(Request(Method.PUT, keyUrl!!
            .query("partNumber", "2")
            .query("uploadId", uploadId))
            .body(contentOriginal.byteInputStream(), contentOriginal.length.toLong())
        )
        assertThat("Second part upload", secondPart.status, equalTo(Status.OK))
        val etag2 = secondPart.header("ETag")!!

        /* finalise multipart */
        val finaliseUpload = client(Request(Method.POST, keyUrl!!.query("uploadId", uploadId))
            .body(listOf(etag1, etag2).toCompleteMultipartUploadXml()))
        assertThat("Finalize of key should succeed", finaliseUpload.status, equalTo(Status.OK))

        assertThat(
            "Key should appear in bucket listing",
            client(Request(Method.GET, bucketUrl!!)).bodyString(),
            containsSubstring(key!!))
        assertThat(
            "Key contents should be as expected",
            client(Request(Method.GET, keyUrl!!)).bodyString(),
            equalTo(contentOriginal + contentOriginal))
        assertThat(
            "Delete of key should succeed",
            client(Request(Method.DELETE, keyUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            client(Request(Method.GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))
        assertThat(
            "Delete of bucket should succeed",
            client(Request(Method.DELETE, bucketUrl!!)).status,
            equalTo(Status.NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            client(Request(Method.GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
    }

    @After
    fun removeBucket() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(ApacheClient())

        client(Request(Method.DELETE, bucketUrl!!))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun checkPropertiesExist() {
            Assume.assumeTrue(properties() != null)
        }

        private fun properties(): InputStream? {
            return AwsRealMultipartTest::class.java.getResourceAsStream("/aws.properties")
        }
    }
}

private fun Response.uploadId(): String? = Regex(""".*UploadId>(.+)</UploadId.*""").find(bodyString())?.groupValues?.get(1)

private fun List<String>.toCompleteMultipartUploadXml(): String {
    val parts = mapIndexed { index, etag ->
        """<Part><PartNumber>${index + 1}</PartNumber><ETag>$etag</ETag></Part>"""
    }.joinToString("")
    return """<CompleteMultipartUpload>$parts</CompleteMultipartUpload>"""
}
