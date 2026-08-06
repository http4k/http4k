package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.Payload.Mode.Signed
import java.time.Clock

/**
 * Standard HTTP implementation of IotJobsDataPlane. The endpoint defaults to
 * https://data.jobs.iot.<region>.amazonaws.com - it cannot be derived from the companion,
 * which carries the service's SigV4 signing name (`iot-jobs-data`) instead.
 */
fun IotJobsDataPlane.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : IotJobsDataPlane {
    private val signedHttp = signAwsRequests(
        region, credentialsProvider, clock, Signed,
        overrideEndpoint ?: Uri.of("https://data.jobs.iot.$region.amazonaws.com")
    ).then(http)

    override fun <R> invoke(action: IotJobsDataPlaneAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a IotJobsDataPlane from a System environment
 */
fun IotJobsDataPlane.Companion.Http(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)

/**
 * Convenience function to create a IotJobsDataPlane from an http4k Environment
 */
fun IotJobsDataPlane.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(AWS_REGION(env), credentialsProvider, http, clock, overrideEndpoint)
