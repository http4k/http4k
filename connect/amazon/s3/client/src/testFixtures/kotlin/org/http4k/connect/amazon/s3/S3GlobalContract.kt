package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

interface S3GlobalContract : AwsContract {
    private val s3
        get() =
        S3.Http({ aws.credentials }, http)

    private val s3Bucket
        get() =
        S3Bucket.Http(bucket, aws.region, { aws.credentials }, http)

    private val bucket get() = BucketName.of(uuid().toString())

    @BeforeEach
    fun deleteBucket() {
        s3Bucket.deleteBucket()
    }

    @Test
    fun `bucket lifecycle`() {
        assertThat(s3.listBuckets().successValue().items.contains(bucket), equalTo(false))
        assertThat(s3.createBucket(bucket, aws.region), equalTo(Success(Unit)))
        try {
            assertThat(s3.listBuckets().successValue().items.contains(bucket), equalTo(true))
            assertThat(s3Bucket.deleteBucket(), equalTo(Success(Unit)))
            assertThat(s3.listBuckets().successValue().items.contains(bucket), equalTo(false))
        } finally {
            s3Bucket.deleteBucket().successValue()
        }
    }

    @Test
    fun `create bucket in default (us-east-1) region`() {
        assertThat(s3.createBucket(bucket, Region.US_EAST_1), equalTo(Success(Unit)))

        val s3Bucket = S3Bucket.Http(bucket, Region.US_EAST_1, { aws.credentials }, http)
        s3Bucket.deleteBucket().successValue()
    }
}

