package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment

fun interface CredentialsChain : () -> AwsCredentials? {
    infix fun orElse(next: CredentialsChain) = CredentialsChain { this() ?: next() }
    fun provider() = CredentialsProvider {
        this() ?: throw IllegalArgumentException("Could not find any valid credentials in the chain")
    }

    companion object
}

fun CredentialsChain.Companion.Environment(env: Environment) = CredentialsChain {
    val accessKey = AWS_ACCESS_KEY_ID_OPTIONAL(env)
    val secretKey = AWS_SECRET_ACCESS_KEY_OPTIONAL(env)
    if (accessKey == null || secretKey == null) null else {
        AwsCredentials(
            accessKey = accessKey.value,
            secretKey = secretKey.value,
            sessionToken = AWS_SESSION_TOKEN(env)?.value
        )
    }
}
