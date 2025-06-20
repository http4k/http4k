package org.http4k.connect.amazon.instancemetadata

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.onFailure
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.instancemetadata.model.Token
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

typealias Ec2InstanceMetadataTokenProvider = () -> Token

fun refreshingEc2InstanceMetadataTokenProvider(
    clock: Clock = Clock.systemUTC(),
    tokenTtl: Duration = Duration.ofMinutes(5),
    gracePeriod: Duration = Duration.ofSeconds(30),
    http: HttpHandler = JavaHttpClient()
): Ec2InstanceMetadataTokenProvider {
    val token = AtomicReference<TokenContainer>(null)

    fun refresh(): Token = synchronized(token) {
        val current = token.get()
        when {
            current != null && !current.expiresWithin(clock, gracePeriod) -> current.token
            else -> when (val refresh = getToken(tokenTtl, http)) {
                is Success<Token> -> refresh.value.also {
                    token.set(TokenContainer(it, clock.instant().plus(tokenTtl)))
                }
                is Failure<RemoteFailure> -> refresh.reason.throwIt()
            }
        }
    }

    return {
        token.get()
            ?.takeIf { !it.expiresWithin(clock, gracePeriod) }
            ?.token
            ?: refresh()
    }
}

fun staticEc2InstanceMetadataTokenProvider(
    tokenTtl: Duration = Duration.ofMinutes(5),
    http: HttpHandler = JavaHttpClient()
): Ec2InstanceMetadataTokenProvider {
    val token = getToken(tokenTtl, http).onFailure { it.reason.throwIt() }
    return { token }
}

private fun getToken(ttl: Duration, http: HttpHandler): Result4k<Token, RemoteFailure> {
    val request = Request(Method.PUT, "http://169.254.169.254/latest/api/token")
        .header("X-aws-ec2-metadata-token-ttl-seconds", ttl.toSeconds().toString())

    val response = http(request)

    return if (response.status.successful) {
        Success(response.value(Token))
    } else {
        Failure(RemoteFailure(request.method, request.uri, response.status, response.bodyString()))
    }
}

private data class TokenContainer(
    val token: Token,
    val expires: Instant
) {
    fun expiresWithin(clock: Clock, duration: Duration): Boolean =
        expires
            .minus(duration)
            .isBefore(clock.instant())
}
