package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.Payload
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import java.io.InputStream
import java.util.*

abstract class AbstractAwsRealS3TestCase {
    var bucketName: String? = null
    var key: String? = null
    var bucketUrl: Uri? = null
    var keyUrl: Uri? = null
    var s3Root: Uri? = null
    var scope: AwsCredentialScope? = null
    var credentials: AwsCredentials? = null

    @Before
    fun setup() {
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

    @After
    fun removeBucket() {
        val client = awsClientFilter(Payload.Mode.Signed)
            .then(ApacheClient())

        client(Request(Method.DELETE, bucketUrl!!))
    }

    fun awsClientFilter(signed: Payload.Mode) = ClientFilters.AwsAuth(scope!!, credentials!!, payloadMode = signed)

    companion object {
        @BeforeClass
        @JvmStatic
        fun checkPropertiesExist() {
            Assume.assumeTrue(properties() != null)
        }

        private fun properties(): InputStream? {
            return AbstractAwsRealS3TestCase::class.java.getResourceAsStream("/aws.properties")
        }
    }
}