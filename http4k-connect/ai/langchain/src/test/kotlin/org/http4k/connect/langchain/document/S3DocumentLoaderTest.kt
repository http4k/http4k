package org.http4k.connect.langchain.document

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.valueOrNull
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.Metadata
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_ACCESS_KEY_ID
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.AWS_SECRET_ACCESS_KEY
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Region.Companion.US_EAST_1
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.s3.FakeS3
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.createBucket
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.putObject
import org.junit.jupiter.api.Test

class S3DocumentLoaderTest {
    private val env = Environment.defaults(
        AWS_REGION of US_EAST_1,
        AWS_ACCESS_KEY_ID of AccessKeyId.of("123"),
        AWS_SECRET_ACCESS_KEY of SecretAccessKey.of("123")
    )
    private val bucket = BucketName.of("hello")

    private val s3 = FakeS3().apply {
        s3Client().createBucket(bucket, US_EAST_1).valueOrNull()!!
    }
    private val key1 = BucketKey.of("1")
    private val key2 = BucketKey.of("2")

    init {
        S3Bucket.Http(bucket, US_EAST_1, env = env, http = s3).apply {
            putObject(key1, key1.value.byteInputStream()).valueOrNull()!!
            putObject(key2, key2.value.byteInputStream()).valueOrNull()!!
        }
    }

    private val s3DocumentLoader = S3DocumentLoader(env, http = s3)
    private val parser = DocumentParser { Document(it.reader().readText()) }

    @Test
    fun `can load document`() {
        assertThat(
            s3DocumentLoader(bucket, parser), equalTo(
                Success(
                    listOf(
                        Document("1", Metadata(mapOf("source" to "s3://hello/1"))),
                        Document("2", Metadata(mapOf("source" to "s3://hello/2"))),
                    )
                )
            )
        )
    }
}
