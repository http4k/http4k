package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.RfcTimestamp
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.fakeAwsEnvironment
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

class FakeS3ObjectTaggingTest {

    private val bucket = BucketName.of(UUID.randomUUID().toString())
    private val key = BucketKey.of("originalKey")

    private var time = Instant.parse("2024-06-27T12:00:00Z")
    private val clock = object: Clock() {
        override fun instant() = time
        override fun withZone(zone: ZoneId?) = TODO()
        override fun getZone() = ZoneOffset.UTC
    }

    private val s3Bucket = FakeS3(clock = clock)
        .also { it.s3Client().createBucket(bucket, fakeAwsEnvironment.region).successValue() }
        .s3BucketClient(bucket, fakeAwsEnvironment.region)

    @Test
    fun `put object tagging updates last-modified`() {
        s3Bucket.putObject(key, "hello".byteInputStream())

        time += Duration.ofMinutes(1)
        s3Bucket.putObjectTagging(key, listOf(Tag("hello", "there")))
        assertThat(s3Bucket.headObject(key).successValue()?.lastModified, equalTo(RfcTimestamp.of(time)))
    }

    @Test
    fun `delete object tagging updates last-modified`() {
        s3Bucket.putObject(key, "hello".byteInputStream(), tags = listOf(Tag("hello", "there")))

        time += Duration.ofMinutes(1)
        s3Bucket.deleteObjectTagging(key)
        assertThat(s3Bucket.headObject(key).successValue()?.lastModified, equalTo(RfcTimestamp.of(time)))
    }
}
