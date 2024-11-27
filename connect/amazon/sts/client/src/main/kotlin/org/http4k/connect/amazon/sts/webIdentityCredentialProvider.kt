package org.http4k.connect.amazon.sts

import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.AWS_ROLE_ARN
import org.http4k.connect.amazon.AWS_ROLE_SESSION_NAME
import org.http4k.connect.amazon.AWS_WEB_IDENTITY_TOKEN
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.connect.amazon.sts.action.AssumeRoleWithWebIdentity
import org.http4k.core.HttpHandler
import java.time.Clock
import java.time.Duration

/**
 * Assume STS role using WebIdentityTokem
 */
fun CredentialsProvider.Companion.STSWebIdentity(
    region: Region,
    roleArn: ARN,
    webIdentityToken: () -> WebIdentityToken,
    roleSessionName: () -> RoleSessionName? = { null },
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300)
) = CredentialsProvider.STS(
    STS.Http(region, { AwsCredentials("", "") }, http, clock),
    clock, gracePeriod
) {
    AssumeRoleWithWebIdentity(
        roleArn,
        roleSessionName() ?: RoleSessionName.of("http4k-connect-" + clock.millis()),
        webIdentityToken()
    )
}

fun CredentialsProvider.Companion.STSWebIdentity(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300)
) = STSWebIdentity(Environment.from(env), http, clock, gracePeriod)

fun CredentialsProvider.Companion.STSWebIdentity(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = Duration.ofSeconds(300)
) = STSWebIdentity(
    AWS_REGION(env),
    AWS_ROLE_ARN(env),
    { AWS_WEB_IDENTITY_TOKEN(env) },
    { AWS_ROLE_SESSION_NAME(env) },
    http, clock, gracePeriod
)
