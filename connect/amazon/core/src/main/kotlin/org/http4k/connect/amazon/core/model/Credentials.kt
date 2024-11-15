package org.http4k.connect.amazon.core.model

import org.http4k.aws.AwsCredentials
import se.ansman.kotshi.JsonSerializable
import java.time.Clock
import java.time.Duration

@JsonSerializable
data class Credentials(
    val Token: SessionToken,
    val AccessKeyId: AccessKeyId,
    val SecretAccessKey: SecretAccessKey,
    val Expiration: Expiration,
    val RoleArn: ARN?
) {
    fun expiresWithin(clock: Clock, duration: Duration): Boolean =
        Expiration.value.toInstant()
            .minus(duration)
            .isBefore(clock.instant())

    fun asHttp4k() = AwsCredentials(AccessKeyId.value, SecretAccessKey.value, Token.value)
}
