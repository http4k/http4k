package org.http4k.connect.amazon.cognito

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
import java.lang.System.getenv
import java.time.Clock

/**
 * Standard HTTP implementation of Cognito
 */
fun Cognito.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : Cognito {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)

    override fun <R : Any> invoke(action: CognitoAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a Cognito from a System environment
 */
fun Cognito.Companion.Http(
    env: Map<String, String> = getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env)
) = Http(Environment.from(env), http, clock, credentialsProvider)

/**
 * Convenience function to create a Cognito from an http4k Environment
 */
fun Cognito.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env)
) = Http(AWS_REGION(env), credentialsProvider, http, clock)
