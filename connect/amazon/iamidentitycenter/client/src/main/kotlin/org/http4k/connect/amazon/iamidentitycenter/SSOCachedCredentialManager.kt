package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.DeviceToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisteredClient
import org.http4k.connect.model.Timestamp
import org.http4k.format.AwsCoreMoshi
import se.ansman.kotshi.JsonSerializable
import java.nio.file.Path
import java.time.Clock
import java.time.Instant


fun retrieveSSOCachedCredentials(cachedTokenPath: Path): SSOCachedCredentials? {
    val file = cachedTokenPath.toFile()
    return if (!file.exists() || !file.isFile) null else AwsCoreMoshi.asA<SSOCachedCredentials>(file.readText())
}

fun storeSSOCachedCredentials(cachedTokenPath: Path, cachedCredentials: SSOCachedCredentials) {
    cachedTokenPath.toFile().writeText(AwsCoreMoshi.asFormatString(cachedCredentials))
}

@JsonSerializable
data class SSOCachedCredentials(
    val startUrl: String,
    val region: String,
    val accessToken: String,
    val expiresAt: Instant,
    val clientId: String,
    val clientSecret: String,
    val registrationExpiresAt: Instant
) {
    companion object
}

fun SSOCachedCredentials.toDeviceToken() = DeviceToken(
    accessToken = AccessToken.of(accessToken),
    expiresIn = expiresAt.epochSecond,
    idToken = null,
    refreshToken = null,
    aws_sso_app_session_id = null,
    originSessionId = null,
    issuedTokenType = null,
    tokenType = "Bearer",
)

fun SSOCachedCredentials.toRegisteredClient(clock: Clock) = RegisteredClient(
    clientId = ClientId.of(clientId),
    clientSecret = ClientSecret.of(clientSecret),
    clientIdIssuedAt = Timestamp.of(Instant.now(clock)),
    clientSecretExpiresAt = Timestamp.of(registrationExpiresAt),
    tokenEndpoint = null,
    authorizationEndpoint = null
)

fun SSOCachedCredentials.Companion.of(
    ssoProfile: SSOProfile,
    deviceToken: DeviceToken,
    client: RegisteredClient
) = SSOCachedCredentials(
    ssoProfile.startUri.toString(),
    ssoProfile.region.value,
    deviceToken.accessToken.value,
    expiresAt = Instant.ofEpochSecond(deviceToken.expiresIn),
    clientId = client.clientId.value,
    clientSecret = client.clientSecret.value,
    registrationExpiresAt = client.clientSecretExpiresAt.toInstant()
)


