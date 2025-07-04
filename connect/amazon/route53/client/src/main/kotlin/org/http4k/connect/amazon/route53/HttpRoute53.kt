package org.http4k.connect.amazon.route53

import org.http4k.aws.AwsCredentialScope
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.route53.action.Route53Action
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.AwsAuth
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.SetHostFrom
import org.http4k.filter.ClientFilters.SetXForwardedHost
import java.time.Clock
import java.time.Clock.systemUTC

/**
 * Standard HTTP implementation of S3
 */
fun Route53.Companion.Http(
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : Route53 {
    private val signedHttp = SetHostFrom(overrideEndpoint ?: Uri.of("https://route53.amazonaws.com"))
        .then(SetXForwardedHost())
        .then(
            ClientFilters.AwsAuth(
                AwsCredentialScope("us-east-1", awsService.value),
                credentialsProvider, clock
            )
        )
        .then(http)

    override fun <R: Any> invoke(action: Route53Action<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a SNS from a System environment
 */
fun Route53.Companion.Http(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)

/**
 * Convenience function to create a SNS from an http4k Environment
 */
fun Route53.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(credentialsProvider, http, clock, overrideEndpoint)
