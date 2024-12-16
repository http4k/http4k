package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.connect.amazon.iamidentitycenter.model.AccessToken
import org.http4k.connect.amazon.iamidentitycenter.model.ClientId
import org.http4k.connect.amazon.iamidentitycenter.model.ClientSecret
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.model.cachedRegistrationPath
import org.http4k.connect.amazon.iamidentitycenter.model.cachedTokenPath
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.DeviceToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisteredClient
import org.http4k.connect.model.Timestamp
import org.http4k.format.AwsCoreMoshi
import se.ansman.kotshi.JsonSerializable
import java.nio.file.Path
import java.time.Clock
import java.time.Instant

class SSOCacheManager(val cachedTokenDirectory: Path) {

    private fun SSOProfile.cachedTokenPath() = cachedTokenPath(cachedTokenDirectory)
    private fun SSOProfile.cachedRegistrationPath() = cachedRegistrationPath(cachedTokenDirectory)

    fun retrieveSSOCachedToken(ssoProfile: SSOProfile): SSOCachedToken? {
        val file = ssoProfile.cachedTokenPath().toFile()
        return if (!file.exists() || !file.isFile) null else AwsCoreMoshi.asA<SSOCachedToken>(file.readText())
    }

    fun storeSSOCachedToken(ssoProfile: SSOProfile, cachedToken: SSOCachedToken) {
        ssoProfile.cachedTokenPath().toFile().writeText(AwsCoreMoshi.asFormatString(cachedToken))
    }

    fun retrieveSSOCachedRegistration(ssoProfile: SSOProfile): SSOCachedRegistration? {
        val file = ssoProfile.cachedRegistrationPath().toFile()
        return if (!file.exists() || !file.isFile) null else AwsCoreMoshi.asA<SSOCachedRegistration>(file.readText())
    }

    fun storeSSOCachedRegistration(ssoProfile: SSOProfile, cachedRegistration: SSOCachedRegistration) {
        ssoProfile.cachedRegistrationPath().toFile().writeText(AwsCoreMoshi.asFormatString(cachedRegistration))
    }
}


@JsonSerializable
data class SSOCachedToken(
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

fun SSOCachedToken.toDeviceToken() = DeviceToken(
    accessToken = AccessToken.of(accessToken),
    expiresIn = expiresAt.epochSecond,
    idToken = null,
    refreshToken = null,
    aws_sso_app_session_id = null,
    originSessionId = null,
    issuedTokenType = null,
    tokenType = "Bearer",
)

fun SSOCachedRegistration.toRegisteredClient(clock: Clock) = RegisteredClient(
    clientId = ClientId.of(clientId),
    clientSecret = ClientSecret.of(clientSecret),
    clientIdIssuedAt = Timestamp.of(Instant.now(clock)),
    clientSecretExpiresAt = Timestamp.of(expiresAt),
    tokenEndpoint = null,
    authorizationEndpoint = null
)

fun SSOCachedToken.Companion.of(
    ssoProfile: SSOProfile,
    deviceToken: DeviceToken,
    client: RegisteredClient
) = SSOCachedToken(
    ssoProfile.startUri.toString(),
    ssoProfile.region.value,
    deviceToken.accessToken.value,
    expiresAt = Instant.ofEpochSecond(deviceToken.expiresIn),
    clientId = client.clientId.value,
    clientSecret = client.clientSecret.value,
    registrationExpiresAt = client.clientSecretExpiresAt.toInstant()
)


@JsonSerializable
data class SSOCachedRegistration(
    val clientId: String,
    val clientSecret: String,
    val expiresAt: Instant
) {
    companion object
}

fun RegisteredClient.toSSOCachedRegistration() = SSOCachedRegistration(
    clientId = clientId.value,
    clientSecret = clientSecret.value,
    expiresAt = clientSecretExpiresAt.toInstant()
)
