package org.http4k.connect.amazon.s3

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.Payload
import java.time.Clock

/**
 * Standard HTTP implementation of S3Bucket
 */
fun S3Bucket.Companion.Http(
    bucketName: BucketName,
    bucketRegion: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    payloadMode: Payload.Mode = Payload.Mode.Signed,
    overrideEndpoint: Uri? = null,
    forcePathStyle: Boolean = false
) = object : S3Bucket {
    private val usePathStyleApi = forcePathStyle || bucketName.requiresPathStyleApi()
    private val pathPrefixToUse = if (usePathStyleApi) "/$bucketName" else ""
    private val bucketDomainToUse = if (usePathStyleApi) "" else "$bucketName."

    private val signedHttp =
        SetBaseUriFrom(Uri.of(pathPrefixToUse))
            .then(
                signAwsRequests(
                    bucketRegion,
                    credentialsProvider,
                    clock,
                    payloadMode,
                    overrideEndpoint,
                    bucketDomainToUse
                )
            )
            .then(http)

    override fun <R> invoke(action: S3BucketAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a S3Bucket from a System environment
 */
fun S3Bucket.Companion.Http(
    bucketName: BucketName,
    bucketRegion: Region,
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    payloadMode: Payload.Mode = Payload.Mode.Signed,
    overrideEndpoint: Uri? = null,
    forcePathStyle: Boolean = false,
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env)
) = Http(bucketName, bucketRegion, credentialsProvider, http, clock, payloadMode, overrideEndpoint, forcePathStyle)

/**
 * Convenience function to create a S3Bucket from an http4k Environment
 */
fun S3Bucket.Companion.Http(
    bucketName: BucketName,
    bucketRegion: Region,
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    payloadMode: Payload.Mode = Payload.Mode.Signed,
    overrideEndpoint: Uri? = null,
    forcePathStyle: Boolean = false,
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env)
) = Http(bucketName, bucketRegion, credentialsProvider, http, clock, payloadMode, overrideEndpoint, forcePathStyle)
