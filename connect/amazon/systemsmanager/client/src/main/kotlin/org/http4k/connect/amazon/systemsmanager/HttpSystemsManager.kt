package org.http4k.connect.amazon.systemsmanager

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
import java.time.Clock.systemUTC

/**
 * Standard HTTP implementation of SystemsManager
 */
fun SystemsManager.Companion.Http(
    region: Region,
    credentialsProvider: CredentialsProvider,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    overrideEndpoint: Uri? = null,
) = object : SystemsManager {
    private val signedHttp = signAwsRequests(region, credentialsProvider, clock, Signed, overrideEndpoint).then(http)

    override fun <R : Any> invoke(action: SystemsManagerAction<R>) = action.toResult(signedHttp(action.toRequest()))
}

/**
 * Convenience function to create a SystemsManager from a System environment
 */
fun SystemsManager.Companion.Http(
    env: Map<String, String> = getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(Environment.from(env), http, clock, credentialsProvider, overrideEndpoint)


/**
 * Convenience function to create a SystemsManager from an http4k Environment
 */
fun SystemsManager.Companion.Http(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = systemUTC(),
    credentialsProvider: CredentialsProvider = CredentialsProvider.Environment(env),
    overrideEndpoint: Uri? = null,
) = Http(AWS_REGION(env), credentialsProvider, http, clock, overrideEndpoint)
