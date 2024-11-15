package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.format.AwsCoreMoshi
import se.ansman.kotshi.JsonSerializable
import java.io.File
import java.time.Clock
import java.time.Clock.systemUTC
import kotlin.io.path.Path

fun CredentialsChain.Companion.CliCache(clock: Clock = systemUTC()) = CredentialsChain {
    val dir = Path(System.getProperty("user.home")).resolve(".aws/cli/cache").toFile()
    if (!dir.exists() || !dir.isDirectory) null
    else {
        dir.listFiles()
            ?.map(File::readText)
            ?.map { AwsCoreMoshi.asA<CliCachedCredentialsFile>(it) }
            ?.filter { it.ProviderType == "sso" }
            ?.firstOrNull { it.Credentials.Expiration.value.toInstant().isAfter(clock.instant()) }
            ?.Credentials
            ?.let { AwsCredentials(it.AccessKeyId.value, it.SecretAccessKey.value, it.SessionToken.value) }
    }
}

@JsonSerializable
data class CliCachedCredentials(
    val AccessKeyId: AccessKeyId,
    val SecretAccessKey: SecretAccessKey,
    val SessionToken: SessionToken,
    val Expiration: Expiration
)

@JsonSerializable
data class CliCachedCredentialsFile(
    val Credentials: CliCachedCredentials,
    val ProviderType: String? = null
)
