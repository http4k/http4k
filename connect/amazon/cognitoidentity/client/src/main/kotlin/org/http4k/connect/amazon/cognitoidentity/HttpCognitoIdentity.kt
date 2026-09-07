package org.http4k.connect.amazon.cognitoidentity

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
 * Standard HTTP implementation of CognitoIdentity
 */
fun CognitoIdentity.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : CognitoIdentity {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)

    override fun <R : Any> invoke(action: CognitoIdentityAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a CognitoIdentity from a System environment
 */
fun CognitoIdentity.Companion.Http(
    env: Map<String, String> = getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(Environment.from(env), http, clock, credentialsProvider)

/**
 * Convenience function to create a CognitoIdentity from an http4k Environment
 */
fun CognitoIdentity.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(AWS_REGION(env), credentialsProvider, http, clock)
