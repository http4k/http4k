package org.http4k.connect.amazon.sts

import dev.forkhandles.result4k.onFailure
import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_CREDENTIAL_PROFILES_FILE
import org.http4k.connect.amazon.AWS_PROFILE
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsProfile
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.sts.action.AssumeRole
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

private fun AwsProfile.assumeRole(
    profiles: Map<ProfileName, AwsProfile>,
    clock: Clock,
    getStsClient: (AwsCredentials) -> STS
): Credentials? {
    val roleArn = roleArn ?: return null
    val sourceProfile = sourceProfileName
        ?.let { profiles[it] }
        ?: return null

    val sourceCredentials = sourceProfile
        .assumeRole(profiles, clock, getStsClient)?.asHttp4k()
        ?: sourceProfile.getCredentials()
        ?: return null

    val sts = getStsClient(sourceCredentials)
    val sessionName = roleSessionName ?: RoleSessionName.of("http4k-connect-" + clock.millis())
    return sts(AssumeRole(roleArn, sessionName))
        .onFailure { it.reason.throwIt() }
        .Credentials
}

private data class ExpiringCredentials(val credentials: AwsCredentials, val expires: Instant?) {
    fun expiresWithin(clock: Clock, duration: Duration): Boolean {
        if (expires == null) return false
        return expires.minus(duration).isBefore(clock.instant())
    }
}

// TODO support web identity
fun CredentialsChain.Companion.StsProfile(
    credentialsPath: Path,
    profileName: ProfileName,
    getStsClient: (AwsCredentials) -> STS,
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300),
) = object : CredentialsChain {
    private val credentials = AtomicReference<ExpiringCredentials>(null)

    override fun invoke(): AwsCredentials? {
        val current = credentials.get()
            ?.takeIf { !it.expiresWithin(clock, gracePeriod) }
            ?: refresh()
        return current?.credentials
    }

    private fun refresh(): ExpiringCredentials? = synchronized(credentials) {
        val current = credentials.get()
        when {
            current != null && !current.expiresWithin(clock, gracePeriod) -> current
            else -> getCredentials()
        }
    }

    private fun getCredentials(): ExpiringCredentials? {
        val profiles = AwsProfile.loadProfiles(credentialsPath)
        return profiles[profileName]?.let {
            it.getCredentials()
                ?.let { ExpiringCredentials(it, null) }
                ?: it.assumeRole(profiles, clock, getStsClient)
                    ?.let { ExpiringCredentials(it.asHttp4k(), it.Expiration.value.toInstant()) }
        }

    }
}

fun CredentialsChain.Companion.StsProfile(env: Environment = Environment.ENV) = CredentialsChain.StsProfile(
    credentialsPath = AWS_CREDENTIAL_PROFILES_FILE(env),
    profileName = AWS_PROFILE(env),
    getStsClient = { credentials -> STS.Http(env, credentialsProvider = { credentials }) }
)

fun CredentialsProvider.Companion.StsProfile(env: Environment = Environment.ENV) =
    CredentialsChain.StsProfile(env).provider()
