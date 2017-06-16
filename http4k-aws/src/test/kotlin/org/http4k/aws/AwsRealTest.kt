package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.util.*

class AwsRealTest {
    private var client: HttpHandler? = null
    private var bucketName: String? = null
    private var key: String? = null
    private var bucketUrl: Uri? = null
    private var keyUrl: Uri? = null
    private var s3Root: Uri? = null

    @Before
    @Throws(IOException::class)
    fun createClient() {
        val properties = Properties()
        properties.load(properties())

        assertThat(
            "Developer should understand what this test does- set signMyLifeAway property to the expected value.",
            properties.getProperty("signMyLifeAway"),
            equalTo("I've checked the code of this test and understand that it creates and deletes buckets and keys using my credentials"))

        client = DebuggingFilters.PrintRequestAndResponse()
            .then(ClientFilters.AwsAuth(AwsCredentialScope(properties.getProperty("region"), properties.getProperty("service")),
                AwsCredentials(properties.getProperty("accessKey"), properties.getProperty("secretKey")))
                .then(ApacheClient()))

        bucketName = UUID.randomUUID().toString()
        key = UUID.randomUUID().toString()
        bucketUrl = Uri.of("https://$bucketName.s3.amazonaws.com/")
        keyUrl = Uri.of("https://$bucketName.s3.amazonaws.com/$key")
        s3Root = Uri.of("https://s3.amazonaws.com/")
    }

    @Test
    @Throws(Exception::class)
    fun putThenGetThenDelete() {
        val contents = UUID.randomUUID().toString()

        assertThat(
            "Bucket should not exist in root listing",
            client!!(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
        assertThat(
            "Put of bucket should succeed",
            client!!(Request(PUT, bucketUrl!!)).status,
            equalTo(OK))
        assertThat(
            "Bucket should exist in root listing",
            client!!(Request(GET, s3Root!!)).bodyString(),
            containsSubstring(bucketName!!))
        assertThat(
            "Key should not exist in bucket listing",
            client!!(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))
        assertThat(
            "Put of key should succeed",
            client!!(Request(PUT, keyUrl!!).body(contents)).status,
            equalTo(OK))
        assertThat(
            "Key should appear in bucket listing",
            client!!(Request(GET, bucketUrl!!)).bodyString(),
            containsSubstring(key!!))
        assertThat(
            "Key contents should be as expected",
            client!!(Request(GET, keyUrl!!)).bodyString(),
            equalTo(contents))
        assertThat(
            "Delete of key should succeed",
            client!!(Request(DELETE, keyUrl!!)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Key should no longer appear in bucket listing",
            client!!(Request(GET, bucketUrl!!)).bodyString(),
            !containsSubstring(key!!))
        assertThat(
            "Delete of bucket should succeed",
            client!!(Request(DELETE, bucketUrl!!)).status,
            equalTo(NO_CONTENT))
        assertThat(
            "Bucket should no longer exist in root listing",
            client!!(Request(GET, s3Root!!)).bodyString(),
            !containsSubstring(bucketName!!))
    }

    @After
    @Throws(Exception::class)
    fun removeBucket() {
        client!!(Request(DELETE, bucketUrl!!))
    }

    companion object {
        @BeforeClass @JvmStatic
        fun checkPropertiesExist() {
            assumeTrue(properties() != null)
        }

        private fun properties(): InputStream? {
            return AwsRealTest::class.java.getResourceAsStream("aws.properties")
        }
    }
}

