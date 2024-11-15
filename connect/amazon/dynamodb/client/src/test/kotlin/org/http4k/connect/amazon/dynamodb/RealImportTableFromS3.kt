package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.RealAwsContract
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.createBucket
import org.http4k.connect.amazon.s3.deleteBucket
import org.http4k.connect.amazon.s3.deleteObject
import org.http4k.connect.amazon.s3.listBuckets
import org.http4k.connect.amazon.s3.listObjectsV2
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.putObject
import org.http4k.connect.successValue
import org.junit.jupiter.api.Disabled
import java.time.Duration

@Disabled
class RealImportTableFromS3Test : ImportTableFromS3Contract, RealAwsContract {

    override fun initBucket(bucketName: BucketName, csv: String) {
        bucketName.create()
        bucketName.uploadCsv(csv)
    }

    override fun cleanupBucket(bucketName: BucketName) {
        bucketName.delete()
    }

    private fun BucketName.create() {
        val s3 = S3.Http({ aws.credentials }, http)
        s3.createBucket(this, aws.region).successValue()
        s3.waitForBucketCreated(this)
    }

    private fun BucketName.uploadCsv(csv: String) {
        val s3Bucket = S3Bucket.Http(this, aws.region, { aws.credentials }, http)
        s3Bucket.putObject(
            key = BucketKey.of("data.csv"),
            content = csv.byteInputStream(),
            headers = emptyList()
        ).successValue()
    }

    private fun BucketName.delete() {
        with(S3Bucket.Http(this, aws.region, { aws.credentials }, http)) {
            listObjectsV2().successValue().forEach { deleteObject(it.Key) }
            deleteBucket()
        }
    }
}

private fun S3.waitForBucketCreated(bucketName: BucketName, timeout: Duration = Duration.ofSeconds(10)) {
    waitUntil(
        { listBuckets().successValue().items.contains(bucketName) },
        failureMessage = "Bucket $bucketName was not created after $timeout",
        timeout = timeout
    )
}
