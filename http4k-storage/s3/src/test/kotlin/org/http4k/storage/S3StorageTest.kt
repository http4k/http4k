package org.http4k.storage

import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.FakeS3
import org.http4k.connect.amazon.s3.action.CreateBucket
import org.http4k.connect.amazon.s3.model.BucketName

class S3StorageTest : StorageContract() {
    override val storage by lazy {
        val bucketName = BucketName.of("foobar")
        val region = Region.of("ldn-north-1")
        val fakeS3 = FakeS3().apply {
            this.s3Client()(CreateBucket(bucketName, region))
        }
        Storage.S3(fakeS3.s3BucketClient(bucketName, region))
    }
}
