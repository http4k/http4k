package org.http4k.connect.amazon.sts

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.AWS_ROLE_ARN
import org.http4k.connect.amazon.AWS_ROLE_SESSION_NAME
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.sts.action.AssumeRole
import org.http4k.connect.amazon.sts.action.AssumedRole
import org.http4k.core.HttpHandler
import java.time.Clock
import java.time.Duration
import java.time.Duration.ofSeconds
import java.util.concurrent.atomic.AtomicReference

/**
 * Refreshing credentials provider for getting credentials based on assuming a role in STS.
 */
fun CredentialsProvider.Companion.STS(
    sts: STS,
    clock: Clock,
    gracePeriod: Duration,
    assumeRole: () -> STSAction<out AssumedRole>
) = object : CredentialsProvider {
    private val credentials = AtomicReference<Credentials>(null)

    override fun invoke() =
        (credentials.get()?.takeIf { !it.expiresWithin(clock, gracePeriod) } ?: refresh()).asHttp4k()

    private fun refresh() =
        synchronized(credentials) {
            val current = credentials.get()
            when {
                current != null && !current.expiresWithin(clock, gracePeriod) -> current
                else -> when (val refresh = sts(assumeRole())) {
                    is Success<AssumedRole> -> {
                        val newCreds = refresh.value.Credentials
                        credentials.set(newCreds)
                        newCreds
                    }

                    is Failure<RemoteFailure> -> refresh.reason.throwIt()
                }
            }
        }
}

fun CredentialsProvider.Companion.STS(
    sts: STS,
    roleArn: ARN,
    roleSessionName: RoleSessionName? = null,
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = ofSeconds(300),
) = CredentialsProvider.Companion.STS(
    sts,
    clock, gracePeriod
) {
    AssumeRole(
        roleArn,
        roleSessionName ?: RoleSessionName.of("http4k-connect-" + clock.millis()),
    )
}

fun CredentialsProvider.Companion.STS(
    env: Map<String, String> = System.getenv(),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = ofSeconds(300)
): CredentialsProvider = CredentialsProvider.STS(Environment.from(env), http, clock, gracePeriod)

fun CredentialsProvider.Companion.STS(
    env: Environment,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    gracePeriod: Duration = ofSeconds(300)
) = CredentialsProvider.Companion.STS(
    STS.Http(AWS_REGION(env), CredentialsProvider.Environment(env), http, clock),
    AWS_ROLE_ARN(env),
    AWS_ROLE_SESSION_NAME(env),
    clock, gracePeriod
)
