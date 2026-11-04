package org.http4k.aws

import org.http4k.core.Credentials
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.Payload
import org.http4k.security.CredentialsProvider
import java.time.Clock
import java.time.Duration

/**
 * Returns a CredentialsProvider to authenticate with an AWS RDS database using IAM credentials.
 *
 * Ensure the signing credentials have the required rds-db:connect permission for the RDS DB Resource.
 */
fun rdsIamCredentialsProvider(
    databaseHost: String,
    databasePort: Int,
    databaseUsername: String,
    awsRegion: String,
    awsCredentialsProvider: () -> AwsCredentials,
    clock: Clock = Clock.systemUTC()
): CredentialsProvider<Credentials> {
    val signer = AwsRequestPreSigner(
        scope = AwsCredentialScope(awsRegion, "rds-db"),
        credentialsProvider = {
            val credentials = awsCredentialsProvider()
            AwsCredentials(credentials.accessKey, credentials.secretKey, credentials.sessionToken)
        },
        payloadMode = Payload.Mode.Signed,
        clock = clock
    )

    val unsignedRequest = Uri.of("https://$databaseHost")
        .port(databasePort)
        .path("/")
        .let { Request(Method.GET, it) }
        .query("Action", "connect")
        .query("DBUser", databaseUsername)

    return {
        val signed = signer(unsignedRequest, Duration.ofMinutes(15))
        val password = signed.uri.toString().removePrefix("https://")
        Credentials(databaseUsername, password)
    }
}
