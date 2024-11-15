package org.http4k.connect.amazon.s3

import org.http4k.aws.AwsSdkClient
import org.http4k.connect.amazon.configAwsEnvironment
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.StorageClass
import software.amazon.awssdk.services.s3.model.Tier
import java.util.*

class AwsSdkV2ComptibilityTest {

    private val fake = FakeS3()

    private val bucket = UUID.randomUUID().toString()
    private val key = "bar.txt"

    private val client = S3Client.builder()
        .httpClient(AwsSdkClient(fake))
        .credentialsProvider {
            val creds = configAwsEnvironment().credentials
            AwsBasicCredentials.create(creds.accessKey, creds.secretKey)
        }
        .region(Region.US_EAST_1)
        .build()

    @Test
    fun restore() {
        client.createBucket {
            it.bucket(bucket)
        }

        client.putObject({
            it.bucket(bucket)
            it.key(key)
            it.storageClass(StorageClass.GLACIER)
        }, RequestBody.fromString("foo"))

        client.restoreObject {
            it.bucket(bucket)
            it.key(key)
            it.restoreRequest { req ->
                req.days(2)
                req.glacierJobParameters {  param ->
                    param.tier(Tier.EXPEDITED)
                }
            }
        }
    }
}
