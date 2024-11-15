package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.AwsProfile
import org.http4k.connect.amazon.core.model.ProfileName
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference


fun CredentialsChain.Companion.Profile(
    profileName: ProfileName,
    credentialsPath: Path,
): CredentialsChain {
    val cached = AtomicReference<AwsCredentials>(null)

    return CredentialsChain {
        cached.get() ?: synchronized(cached) {
            val profiles = AwsProfile.loadProfiles(credentialsPath)
            val profile = profiles[profileName] ?: return@synchronized null
            val credentials = profile.getCredentials()
            cached.set(credentials)
            credentials
        }
    }
}

fun CredentialsChain.Companion.Profile(
    env: Environment = Environment.ENV,
    profileName: ProfileName = AWS_PROFILE(env),
    credentialsPath: Path = AWS_CREDENTIAL_PROFILES_FILE(env)
) = CredentialsChain.Profile(profileName, credentialsPath)

fun CredentialsProvider.Companion.Profile(
    env: Environment = Environment.ENV,
    profileName: ProfileName = AWS_PROFILE(env),
    credentialsPath: Path = AWS_CREDENTIAL_PROFILES_FILE(env)
) = CredentialsChain.Profile(env, profileName, credentialsPath).provider()
