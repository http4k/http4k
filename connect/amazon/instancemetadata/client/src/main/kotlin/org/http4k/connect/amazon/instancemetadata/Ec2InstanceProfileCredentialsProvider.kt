package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.instancemetadata.model.Ec2Credentials
import org.http4k.core.HttpHandler
import java.time.Clock
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * This provider will time out if not in an EC2 Environment.
 * For that reason, if there are multiple providers in a chain, this provider should be last.
 */
fun CredentialsChain.Companion.Ec2InstanceProfile(
    ec2InstanceMetadata: InstanceMetadataService,
    clock: Clock,
    gracePeriod: Duration
): CredentialsChain {
    val cached = AtomicReference<Ec2Credentials>(null)

    fun refresh() = synchronized(cached) {
        val current = cached.get()
        if (current != null && !current.expiresWithin(clock, gracePeriod)) {
            current
        } else {
            ec2InstanceMetadata
                .listSecurityCredentials()
                .onFailure { it.reason.throwIt() }
                .asSequence()
                .mapNotNull { ec2InstanceMetadata.getSecurityCredentials(it).valueOrNull() }
                .firstOrNull()
                ?.also { cached.set(it) }
        }
    }

    return CredentialsChain {
        val credentials = cached.get()
            ?.takeIf { !it.expiresWithin(clock, gracePeriod) }
            ?: refresh()
        credentials?.asHttp4k()
    }
}

fun CredentialsChain.Companion.Ec2InstanceProfile(
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(30)
) = CredentialsChain.Ec2InstanceProfile(
    ec2InstanceMetadata = InstanceMetadataService.Http(http),
    clock = clock,
    gracePeriod = gracePeriod
)

fun CredentialsProvider.Companion.Ec2InstanceProfile(
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(30)
) = CredentialsChain.Ec2InstanceProfile(
    ec2InstanceMetadata = InstanceMetadataService.Http(http),
    clock = clock,
    gracePeriod = gracePeriod
).provider()
