package org.http4k.aws

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.ApacheClient
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.Payload
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.io.InputStream
import java.util.Properties
import java.util.UUID

abstract class AbstractAwsRealS3TestCase {
    protected lateinit var bucketName: String
    protected lateinit var key: String
    protected lateinit var bucketUrl: Uri
    protected lateinit var keyUrl: Uri
    protected lateinit var s3Root: Uri
    private lateinit var scope: AwsCredentialScope
    private lateinit var credentials: AwsCredentials

    @BeforeEach
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

    @AfterEach
    fun removeBucket() {
        aClient()(Request(DELETE, bucketUrl))
    }


    protected fun aClient() = awsClientFilter(Payload.Mode.Signed)
        .then(ApacheClient())

    protected fun awsClientFilter(signed: Payload.Mode) = ClientFilters.AwsAuth(scope, credentials, payloadMode = signed)

    companion object {
        @BeforeAll
        @JvmStatic
        fun checkPropertiesExist() {
            assumeTrue(properties() != null)
        }

        private fun properties(): InputStream? = AbstractAwsRealS3TestCase::class.java.getResourceAsStream("/aws.properties")
    }
}
