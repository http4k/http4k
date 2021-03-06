package org.http4k.aws

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.DELETE
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.Payload
import org.http4k.lens.LensFailure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.opentest4j.TestAbortedException
import java.util.*

abstract class AbstractAwsRealS3TestCase {
    protected lateinit var bucketName: String
    protected lateinit var key: String
    protected lateinit var bucketUrl: Uri
    protected lateinit var keyUrl: Uri
    protected lateinit var s3Root: Uri

    @BeforeEach
    fun setup() {
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
        .then(JavaHttpClient())

    protected fun awsClientFilter(signed: Payload.Mode) =
        awsCliUserProfiles().profileIfAvailable("http4k-integration-test").awsClientFilterFor("s3", signed)

    private fun AwsCliUserProfiles.profileIfAvailable(name: String) = try {
        profile(name)
    } catch (failure: LensFailure) {
        throw TestAbortedException("Could not load profile: ${failure.message}")
    }
}
