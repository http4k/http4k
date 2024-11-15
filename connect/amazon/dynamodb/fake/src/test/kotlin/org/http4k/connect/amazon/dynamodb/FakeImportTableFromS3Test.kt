package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.dynamodb.endpoints.FakeS3BucketSource
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.connect.amazon.s3.model.BucketName

class FakeImportTableFromS3Test : ImportTableFromS3Contract, FakeAwsContract {
    private val s3BucketSources = mutableListOf<FakeS3BucketSource>()

    private val dynamoDb = FakeDynamoDb(s3BucketSources = { s3BucketSources.toList() })

    override val http = dynamoDb

    override fun initBucket(bucketName: BucketName, csv: String) {
        s3BucketSources += FakeS3BucketSource(name = bucketName.value, csv = csv)
    }

    override fun cleanupBucket(bucketName: BucketName) {
        s3BucketSources.clear()
    }
}
