package org.http4k.connect.amazon.s3

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ClientFilters.SetXForwardedHost
import org.http4k.filter.Payload
import java.lang.System.getenv
import java.time.Clock
import java.time.Clock.systemUTC

/**
 * Standard HTTP implementation of S3
 */
fun S3.Companion.Http(
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    payloadMode: Payload.Mode = Payload.Mode.Signed,
    overrideEndpoint: Uri? = null,
) = object : S3 {
    private val signedHttp = SetHostFrom(overrideEndpoint ?: Uri.of("https://s3.amazonaws.com"))
        .then(SetXForwardedHost())
        .then(
            ClientFilters.AwsAuth(
                AwsCredentialScope("us-east-1", awsService.value),
                credentialsProvider, clock, payloadMode
            )
        )
        .then(http)

    override fun <R> invoke(action: S3Action<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a S3 from a System environment
 */
fun S3.Companion.Http(
    env: Map<String, String> = getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)

/**
 * Convenience function to create a S3 from an http4k Environment
 */
fun S3.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(credentialsProvider, http, clock, overrideEndpoint = overrideEndpoint)
