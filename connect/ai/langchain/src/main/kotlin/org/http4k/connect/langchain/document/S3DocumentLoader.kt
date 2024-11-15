package org.http4k.connect.langchain.document

import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.peek
import dev.langchain4j.data.document.DocumentParser
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.listObjectsV2Paginated
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.Payload
import java.time.Clock

class S3DocumentLoader(
    private val environment: Environment,
    private val credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(environment),
    private val http: HttpHandler = JavaHttpClient(),
    private val clock: Clock = Clock.systemUTC(),
    private val overrideEndpoint: Uri? = null,
    private val forcePathStyle: Boolean = false
) {
    operator fun invoke(bucket: BucketName, key: BucketKey, parser: DocumentParser) =
        s3Client(bucket)[key]
            .map(parser::parse)
            .peek { it.metadata().put("source", "s3://$bucket/$key") }

    operator fun invoke(bucket: BucketName, parser: DocumentParser) = this(bucket, null, parser)

    operator fun invoke(
        bucket: BucketName,
        prefix: String?,
        parser: DocumentParser
    ) = s3Client(bucket)
        .listObjectsV2Paginated(prefix = prefix)
        .map {
            it.map {
                it.map { item ->
                    this(bucket, item.Key, parser)
                        .peek { it.metadata().put("source", "s3://$bucket/${item.Key}") }
                }
                    .allValues()
            }.flatMap { it }
        }
        .allValues()
        .map { it.flatten() }

    private fun s3Client(bucket: BucketName) = S3Bucket.Http(
        bucket,
        AWS_REGION(environment),
        credentialsProvider,
        http,
        clock,
        Payload.Mode.Signed,
        overrideEndpoint,
        forcePathStyle
    )
}
