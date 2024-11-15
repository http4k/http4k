package org.http4k.connect.amazon.containercredentials

import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.peek
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.containercredentials.action.getCredentials
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.lens.LensFailure
import java.time.Clock
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

private fun credentialsProvider(
    containerCredentials: ContainerCredentials,
    uri: Uri,
    clock: Clock,
    gracePeriod: Duration
): () -> Result4k<Credentials, RemoteFailure> {
    val cache = AtomicReference<Credentials>(null)

    fun refresh() = synchronized(cache) {
        cache.get()
            ?.takeIf { !it.expiresWithin(clock, gracePeriod) }
            ?.let { Success(it) }
            ?: containerCredentials.getCredentials(uri).peek(cache::set)
    }

    return {
        cache.get()
            ?.takeIf { !it.expiresWithin(clock, gracePeriod) }
            ?.let { Success(it) }
            ?: refresh()
    }
}

/**
 * Refreshing credentials provider for getting credentials from the container credentials service.
 */
fun CredentialsProvider.Companion.ContainerCredentials(
    containerCredentials: ContainerCredentials,
    uri: Uri,
    clock: Clock,
    gracePeriod: Duration
) = object : CredentialsProvider {
    private val provider = credentialsProvider(containerCredentials, uri, clock, gracePeriod)

    override fun invoke() = provider().onFailure { it.reason.throwIt() }.asHttp4k()
}

fun CredentialsProvider.Companion.ContainerCredentials(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300)
) = CredentialsProvider.ContainerCredentials(
    ContainerCredentials.Http(http, AWS_CONTAINER_AUTHORIZATION_TOKEN(env)),
    AWS_CONTAINER_CREDENTIALS_FULL_URI(env),
    clock, gracePeriod
)

/**
 * Refreshing credentials chain for getting credentials from the container credentials service.
 */
fun CredentialsChain.Companion.ContainerCredentials(
    containerCredentials: ContainerCredentials,
    uri: Uri,
    clock: Clock,
    gracePeriod: Duration
) = object : CredentialsChain {
    private val provider = credentialsProvider(containerCredentials, uri, clock, gracePeriod)

    override fun invoke() = provider.invoke().valueOrNull()?.asHttp4k()
}

fun CredentialsChain.Companion.ContainerCredentials(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300)
): CredentialsChain {
    val (token, uri) = try {
        AWS_CONTAINER_AUTHORIZATION_TOKEN(env) to AWS_CONTAINER_CREDENTIALS_FULL_URI(env)
    } catch (e: LensFailure) {
        return CredentialsChain { null }
    }

    return CredentialsChain.ContainerCredentials(ContainerCredentials.Http(http, token), uri, clock, gracePeriod)
}
