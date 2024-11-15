package org.http4k.connect.amazon.s3

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.or
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.s3.action.ObjectList
import org.http4k.connect.amazon.s3.action.TaggingDirective
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.model.RestoreTier
import org.http4k.connect.amazon.s3.model.S3BucketPreSigner
import org.http4k.connect.amazon.s3.model.StorageClass
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetXForwardedHost
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

interface S3BucketContract : AwsContract {

    val bucket: BucketName
    private val clock get() = Clock.systemUTC()

    val s3Bucket get() = S3Bucket.Http(bucket, aws.region, { aws.credentials }, http, clock)

    private val s3 get() = S3.Http({ aws.credentials }, http)

    val key get() = BucketKey.of("originalKey")

    fun open() {}

    fun close() {}

    @BeforeEach
    fun z_recreate() {
        open()
        System.err.println("Recreating bucket $bucket")
        s3Bucket.deleteObject(key)
        s3Bucket.deleteBucket()
        s3.createBucket(bucket, aws.region).successValue()
    }

    @AfterEach
    fun stopThings() {
        close()
    }

    @Test
    fun `bucket key lifecycle`() {
        waitForBucketCreation()
        try {
            assertThat(s3Bucket.headBucket().successValue(), equalTo(Unit))

            val newKey = BucketKey.of("newKey")

            assertThat(s3Bucket.listObjectsV2().successValue(), equalTo(ObjectList(emptyList())))
            assertThat(s3Bucket[key].successValue(), absent())
            assertThat(s3Bucket.set(key, "hello".byteInputStream()).successValue(), equalTo(Unit))
            assertThat(String(s3Bucket[key].successValue()!!.readBytes()), equalTo("hello"))
            s3Bucket.headObject(key).successValue {
                assertThat(it!!.lastModified, present())
            }

            assertThat(s3Bucket.listObjectsV2().successValue().items.map { it.Key }, equalTo(listOf(key)))
            assertThat(s3Bucket.set(key, "there".byteInputStream()).successValue(), equalTo(Unit))
            assertThat(String(s3Bucket[key].successValue()!!.readBytes()), equalTo("there"))

            s3Bucket.restoreObject(key, 1).failureValue { failure ->
                assertThat(failure.method, equalTo(Method.POST))
                assertThat(failure.uri, equalTo(Uri.of("/$key?restore")))
                assertThat(failure.status, equalTo(Status.FORBIDDEN))
                assertThat(failure.message!!, containsSubstring("<Code>InvalidObjectState</Code>"))
            }

            // copy object (replace tagging)
            assertThat(s3Bucket.copyObject(bucket, key, newKey, taggingDirective = TaggingDirective.REPLACE, tags = listOf(Tag("foo", "bar"))).successValue(), equalTo(Unit))
            assertThat(s3Bucket.headObject(newKey).successValue(), present())
            assertThat(String(s3Bucket[newKey].successValue()!!.readBytes()), equalTo("there"))
            assertThat(s3Bucket.getObjectTagging(newKey).successValue(), equalTo(listOf(Tag("foo", "bar")))) // ensure tags were added during copy
            assertThat(
                s3Bucket.listObjectsV2().successValue().items.map { it.Key },
                equalTo(listOf(key, newKey).sortedBy { it.value })
            )
            assertThat(s3Bucket.deleteObject(newKey).successValue(), equalTo(Unit))

            // copy object (copy tagging)
            assertThat(s3Bucket.copyObject(bucket, key, newKey, tags = listOf(Tag("foo", "bar"))).successValue(), equalTo(Unit))
            assertThat(s3Bucket.getObjectTagging(newKey).successValue(), isEmpty) // ensure new tags were ignored
            assertThat(s3Bucket.deleteObject(newKey).successValue(), equalTo(Unit))

            // delete object
            assertThat(s3Bucket.deleteObject(key).successValue(), equalTo(Unit))
            assertThat(s3Bucket[key].successValue(), equalTo(null))
            assertThat(s3Bucket.listObjectsV2().successValue(), equalTo(ObjectList(emptyList())))

        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `pre signed request`() {
        val preSigner = S3BucketPreSigner(
            bucketName = bucket,
            region = aws.region,
            credentials = aws.credentials,
            clock = clock
        )
        val http = SetXForwardedHost().then(http)

        waitForBucketCreation()
        try {
            preSigner.put(
                key = key,
                headers = listOf("content-type" to "text/plain"),
                duration = Duration.ofMinutes(1)
            ).also {
                val response = Request(PUT, it.uri)
                    .headers(it.signedHeaders)
                    .body("hello there")
                    .let(http)
                assertThat(response, hasStatus(CREATED) or hasStatus(OK))
            }

            preSigner.get(key, Duration.ofMinutes(1)).also {
                val response = Request(GET, it.uri)
                    .headers(it.signedHeaders)
                    .let(http)
                assertThat(response, hasStatus(OK) and hasBody("hello there"))
            }

            preSigner.delete(key, Duration.ofMinutes(1)).also {
                val response = Request(DELETE, it.uri)
                    .headers(it.signedHeaders)
                    .let(http)
                assertThat(response, hasStatus(OK) or hasStatus(NO_CONTENT))
            }
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `glacier lifecycle`() {
        waitForBucketCreation()
        val newKey = BucketKey.of("newKey")
        try {
            // put object into glacier
            s3Bucket.putObject(key, "coldStuff".byteInputStream(), storageClass = StorageClass.GLACIER).successValue()
            s3Bucket.headObject(key).successValue().also { status ->
                assertThat(status?.storageClass, equalTo(StorageClass.GLACIER))
                assertThat(status?.restoreStatus, absent())
            }

            // try to get object from glacier
            s3Bucket[key].failureValue { failure ->
                assertThat(failure.method, equalTo(GET))
                assertThat(failure.uri, equalTo(Uri.of("/$key")))
                assertThat(failure.status, equalTo(Status.FORBIDDEN))
                assertThat(failure.message!!, containsSubstring("<Code>InvalidObjectState</Code>"))
            }

            // try to copy object from glacier
            s3Bucket.copyObject(bucket, key, newKey).failureValue { failure ->
                assertThat(failure.method, equalTo(PUT))
                assertThat(failure.uri, equalTo(Uri.of("/$newKey")))
                assertThat(failure.status, equalTo(Status.FORBIDDEN))
                assertThat(failure.message!!, containsSubstring("<Code>InvalidObjectState</Code>"))
            }

            // restore, head, and get object from glacier
            s3Bucket.restoreObject(key, 2, tier = RestoreTier.Expedited).successValue()
            s3Bucket.waitForRestore(key)
            s3Bucket.headObject(key).successValue().also { status ->
                assertThat(status!!.storageClass, equalTo(StorageClass.GLACIER))
                assertThat(status.restoreStatus!!.ongoingRequest, equalTo(false))
                assertThat(status.restoreStatus!!.expiryDate!!.value, greaterThan(ZonedDateTime.now(clock)))
            }
            assertThat(s3Bucket[key].successValue()?.reader()?.readText(), equalTo("coldStuff"))
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteObject(newKey)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `glacier instant retrieval lifecycle`() {
        waitForBucketCreation()
        try {
            s3Bucket.putObject(key, "lukewarmStuff".byteInputStream(), storageClass = StorageClass.GLACIER_IR)
            s3Bucket.headObject(key).successValue().also {
                assertThat(it?.storageClass, equalTo(StorageClass.GLACIER_IR))
            }
            s3Bucket.restoreObject(key, 1).failureValue { failure ->
                assertThat(failure.method, equalTo(Method.POST))
                assertThat(failure.uri, equalTo(Uri.of("/$key?restore")))
                assertThat(failure.status, equalTo(Status.FORBIDDEN))
                assertThat(failure.message!!, containsSubstring("<Code>InvalidObjectState</Code>"))
            }
            assertThat(s3Bucket[key].successValue()?.reader()?.readText(), equalTo("lukewarmStuff"))
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `glacier deep archive lifecycle`() {
        waitForBucketCreation()
        val newKey = BucketKey.of("newKey")
        try {
            // copy object from standard to glacier DA
            s3Bucket.putObject(key, "warmStuff".byteInputStream()).successValue()
            s3Bucket.copyObject(bucket, key, newKey, StorageClass.DEEP_ARCHIVE)

            s3Bucket.headObject(newKey).successValue().also {
                assertThat(it, present())
                assertThat(it?.storageClass, equalTo(StorageClass.DEEP_ARCHIVE))
            }
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteObject(newKey)
            s3Bucket.deleteBucket()
        }
    }

    @Test
    fun `tag lifecycle`() {
        waitForBucketCreation()

        try {
            // tag operations on missing key
            s3Bucket.putObjectTagging(key, listOf(Tag("foo", "bar"))).failureValue {
                assertThat(it.status, equalTo(NOT_FOUND))
                assertThat(it.message!!, containsSubstring("NoSuchKey"))
            }
            s3Bucket.deleteObjectTagging(key).failureValue {
                assertThat(it.status, equalTo(NOT_FOUND))
                assertThat(it.message!!, containsSubstring("NoSuchKey"))
            }
            s3Bucket.getObjectTagging(key).failureValue {
                assertThat(it.status, equalTo(NOT_FOUND))
                assertThat(it.message!!, containsSubstring("NoSuchKey"))
            }

            // get tags for object with no tags
            s3Bucket.putObject(key, "hello there".byteInputStream()).successValue()
            assertThat(
                s3Bucket.getObjectTagging(key).successValue(),
                equalTo(emptyList())
            )

            // tag operations for object with tags
            s3Bucket.putObject(
                key = key,
                content = "hello there".byteInputStream(),
                tags = listOf(Tag("hello", "there"))
            ).successValue()
            assertThat(
                s3Bucket.getObjectTagging(key).successValue(),
                equalTo(listOf(Tag("hello", "there")))
            )
            s3Bucket.putObjectTagging(key, listOf(Tag("foo", "bar"))).successValue()
            assertThat(
                s3Bucket.getObjectTagging(key).successValue(),
                equalTo(listOf(Tag("foo", "bar")))
            )

            s3Bucket.deleteObjectTagging(key).successValue()
            assertThat(
                s3Bucket.getObjectTagging(key).successValue(),
                equalTo(emptyList())
            )
        } finally {
            s3Bucket.deleteObject(key)
            s3Bucket.deleteBucket()
        }
    }

    open fun waitForBucketCreation() {}

    private fun S3Bucket.waitForRestore(key: BucketKey, timeout: Duration = Duration.ofMinutes(5)) {
        println("Restoring $key... please wait for up to $timeout")
        val start = Instant.now()
        while (Duration.between(start, Instant.now()) < timeout) {
            if (headObject(key).successValue()?.restoreStatus?.ongoingRequest == false) return
            Thread.sleep(Duration.ofSeconds(1))
        }
    }
}
