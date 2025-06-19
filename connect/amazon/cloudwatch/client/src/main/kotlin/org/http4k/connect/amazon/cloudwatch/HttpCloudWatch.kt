package org.http4k.connect.amazon.cloudwatch

import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_CREDENTIALS
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.Payload.Mode.Signed
import java.time.Clock

fun CloudWatch.Companion.Http(
    region: Region,
    credentialsProvider: () -> AwsCredentials,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : CloudWatch {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)

    override fun <R : Any> invoke(action: CloudWatchAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a CloudWatch from a System environment
 */
fun CloudWatch.Companion.Http(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, overrideEndpoint)

/**
 * Convenience function to create a CloudWatch from an http4k Environment
 */
fun CloudWatch.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = Http(AWS_REGION(env), { AWS_CREDENTIALS(env) }, http, clock, overrideEndpoint)
