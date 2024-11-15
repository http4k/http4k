package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.recover
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.s3.TestingHeaders.X_HTTP4K_LAST_MODIFIED
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.debug
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.LastModified
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

class FakeS3BucketTest : S3BucketContract, FakeAwsContract {
    override val http = FakeS3().debug()
    override val bucket = BucketName.of(UUID.randomUUID().toString())

    @Test
    fun `can set last-modified on an object`() {
        try {
            val lastModifiedDate = ZonedDateTime.of(LocalDate.EPOCH, LocalTime.MIDNIGHT, ZoneId.of("GMT"))

            assertThat(
                s3Bucket.putObject(
                    key, "hello".byteInputStream(), listOf(
                        X_HTTP4K_LAST_MODIFIED to LastModified.of(lastModifiedDate).toHeaderValue()
                    )
                ).successValue(), equalTo(Unit)
            )

            assertThat(
                s3Bucket.listObjectsV2().successValue().items.first().LastModified, equalTo(
                    Timestamp.of(lastModifiedDate.toEpochSecond())
                )
            )
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `preserve encoding`() {
        try {

            assertThat(
                s3Bucket.putObject(
                    key, "génial".byteInputStream(Charsets.ISO_8859_1), listOf()
                ).successValue(), equalTo(Unit)
            )

            assertThat(
                s3Bucket[key].successValue()!!.reader(Charsets.ISO_8859_1).readText(), equalTo("génial")
            )
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `can copy object`() {
        try {
            s3Bucket.putObject(key, "hello".byteInputStream(), listOf()).recover { "failed to upload" }
            assertThat(s3Bucket.copyObject(bucket, key, BucketKey.of("hello-copy.txt")).successValue(), equalTo(Unit))
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `can copy object with prefix`() {
        val source = BucketKey.of("source/first/second/hello.txt")
        val destination = BucketKey.of("destination/hello.txt")

        try {
            s3Bucket.putObject(source, "hello".byteInputStream(), listOf()).successValue()
            s3Bucket.copyObject(bucket, source, destination).successValue()

            assertThat(
                s3Bucket[destination]
                    .successValue()!!
                    .reader()
                    .readText(),
                equalTo("hello")
            )
        } finally {
            s3Bucket.deleteObject(source)
            s3Bucket.deleteObject(destination)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `can get object with raw in-memory request`() {
        val key = BucketKey.of("foo.txt")

        try {
            s3Bucket.putObject(key, "hello".byteInputStream(), listOf()).successValue()

            val response = Request(GET, "http://${bucket}.s3.amazonaws.com/foo.txt").let(http)

            assertThat(response, hasStatus(OK))
            assertThat(response, hasBody("hello"))
        } finally {
            s3Bucket.deleteObject(key)
        }
    }
}

class FakeS3BucketPathStyleTest : S3BucketContract, FakeAwsContract {
    override val http = FakeS3()
    override val bucket = BucketName.of(UUID.randomUUID().toString().replace('-', '.'))
}

class FakeS3GlobalTest : S3GlobalContract, FakeAwsContract {
    override val http = FakeS3()
}
