package org.http4k.connect.amazon.s3

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.s3.model.BucketName
import java.util.UUID

class RunningFakeS3BucketTest : S3BucketContract, FakeAwsContract, WithRunningFake(::FakeS3) {
    override val bucket = BucketName.of(UUID.randomUUID().toString())
}
