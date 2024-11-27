package org.http4k.connect.amazon.s3

import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.s3.model.BucketName
import java.util.UUID

class RealS3BucketTest : S3BucketContract, RealAwsContract {
    override val http = JavaHttpClient()


    override fun waitForBucketCreation() {
        Thread.sleep(10000)
    }

    override val bucket: BucketName = BucketName.of(UUID.randomUUID().toString())
}

class RealS3BucketPathStyleTest : S3BucketContract, RealAwsContract {
    override val http = JavaHttpClient()


    override fun waitForBucketCreation() {
        Thread.sleep(10000)
    }

    override val bucket = BucketName.of(UUID.randomUUID().toString().replace('-', '.'))
}

class RealS3GlobalTest : S3GlobalContract, RealAwsContract
