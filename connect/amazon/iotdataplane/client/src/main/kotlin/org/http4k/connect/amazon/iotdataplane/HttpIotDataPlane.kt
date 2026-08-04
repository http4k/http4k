package org.http4k.connect.amazon.iotdataplane

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
 * Standard HTTP implementation of IotDataPlane. The endpoint is account-specific
 * (eg. https://xxxxxxxx-ats.iot.<region>.amazonaws.com) so cannot be derived from the region.
 */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
) = object : IotDataPlane {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, endpoint).then(http)

    override fun <R> invoke(action: IotDataPlaneAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a IotDataPlane from a System environment
 */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(endpoint, Environment.from(env), http, clock, credentialsProvider)

/**
 * Convenience function to create a IotDataPlane from an http4k Environment
 */
fun IotDataPlane.Companion.Http(
    endpoint: Uri,
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(endpoint, AWS_REGION(env), credentialsProvider, http, clock)
