package org.http4k.connect.amazon.cognito.oauth

import dev.forkhandles.result4k.Success
import org.http4k.base64Decoded
import org.http4k.connect.amazon.cognito.Keys
import org.http4k.connect.amazon.core.model.Region
import org.http4k.security.AccessToken
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256
import org.jose4j.jws.JsonWebSignature
import org.jose4j.jwt.JwtClaims
import org.jose4j.jwt.NumericDate
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID

class CognitoAccessTokens(
    private val clock: Clock,
    private val expiry: Duration,
    private val region: Region
) : AccessTokens {
    override fun create(clientId: ClientId, tokenRequest: TokenRequest): Success<AccessToken> {
        val uuid = UUID.nameUUIDFromBytes(clientId.value.toByteArray())
        return coreJwtClaims(uuid, clientId, clock.instant()).toAccessToken()
    }

    override fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest,
    ): Success<AccessToken> {
        val now = clock.instant()

        val email = tokenRequest.authorizationCode.value.base64Decoded()

        val uuid = UUID.nameUUIDFromBytes(email.toByteArray())

        return coreJwtClaims(uuid, clientId, now).apply {
            setClaim(
                "identities", listOf(
                    mapOf(
                        "userId" to uuid.leastSignificantBits.toString(),
                        "providerName" to "Cognito",
                        "providerType" to "Cognito",
                        "issuer" to null,
                        "primary" to "false",
                        "dateCreated" to now.minus(30, DAYS).epochSecond.toString(),
                    )
                )
            )
            setClaim("email_verified", "true")
            setClaim("email", email)
        }.toAccessToken()
    }

    private fun JwtClaims.toAccessToken() = Success(
        AccessToken(
            JsonWebSignature().apply {
                payload = toJson()
                key = Keys.live.second
                algorithmHeaderValue = RSA_USING_SHA256
            }.compactSerialization, expiresIn = expiry.seconds
        )
    )

    private fun coreJwtClaims(uuid: UUID, clientId: ClientId, now: Instant) = JwtClaims().apply {
        subject = uuid.toString()
        issuer = "https://cognito-idp.$region.amazonaws.com/${clientId.value}"
        expirationTime = NumericDate.fromMilliseconds(now.plus(expiry).toEpochMilli())
        issuedAt = NumericDate.fromMilliseconds(now.toEpochMilli())
        setClaim("cognito:username", uuid.toString())
        setClaim("origin_jti", uuid.toString().reversed())
        setClaim("aud", clientId.value)
        setClaim("event_id", uuid.toString().reversed())
        setClaim("token_use", uuid.toString().reversed())
        setClaim("auth_time", now.toEpochMilli())
        setClaim("jti", uuid.toString().reversed())
    }
}
