package org.http4k.connect.amazon.secretsmanager

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
 * Standard HTTP implementation of SecretsManager
 */
fun SecretsManager.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : SecretsManager {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)

    override fun <R : Any> invoke(action: SecretsManagerAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a SecretsManager from a System environment
 */
fun SecretsManager.Companion.Http(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    overrideEndpoint: Uri? = null,
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)

/**
 * Convenience function to create a SecretsManager from an http4k Environment
 */
fun SecretsManager.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(AWS_REGION(env), credentialsProvider, http, clock, overrideEndpoint)
