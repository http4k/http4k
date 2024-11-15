package org.http4k.connect.amazon.evidently

import dev.forkhandles.result4k.Result
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.RemoteFailure
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
 * Standard HTTP implementation
 */
fun Evidently.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : Evidently {
    private val controlHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)
    private val dataHttp =
        signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint, "dataplane.").then(http)

    override fun <R : Any> invoke(action: EvidentlyAction<R>): Result<R, RemoteFailure> {
        val signedHttp = if (action.dataPlane) dataHttp else controlHttp
        return action.toResult(signedHttp(action.toRequest()))
    }
}

/**
 * Convenience function to create a client from a System environment
 */
fun Evidently.Companion.Http(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)

/**
 * Convenience function to create a client from an http4k Environment
 */
fun Evidently.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(AWS_REGION(env), credentialsProvider, http, clock, overrideEndpoint)
