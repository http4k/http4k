package org.http4k.connect.amazon.iamidentitycenter

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.peek
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.DeviceToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisteredClient
import org.http4k.connect.amazon.iamidentitycenter.sso.action.RoleCredentials
import org.http4k.connect.util.WebBrowser
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.nio.file.Path
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path

/**
 * Use SSO to log into the AWS command line using a browser interaction
 */
fun CredentialsProvider.Companion.SSO(
    ssoProfile: SSOProfile,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    clientName: ClientName = ClientName.of("http4k-connect-client"),
    openBrowser: (Uri) -> Any = WebBrowser()::navigateTo,
    waitFor: (Long) -> Unit = { Thread.sleep(it) },
    cachedTokenDirectory: Path = Path(System.getProperty("user.home")).resolve(".aws/sso/cache")
) = object : CredentialsProvider {

    private val ssoCacheManager = SSOCacheManager(cachedTokenDirectory)
    private val ref = AtomicReference<RoleCredentials>(null)

    override fun invoke() = with(
        ref.get()
            ?.takeIf { it.expiration.toInstant().isAfter(clock.instant()) }
            ?: retrieveDeviceToken().getAwsCredentials()
                .peek(ref::set)
                .onFailure { it.reason.throwIt() }
    ) { AwsCredentials(accessKeyId.value, secretAccessKey.value, sessionToken.value) }

    private fun DeviceToken.getAwsCredentials() =
        SSO.Http(ssoProfile.region, http)
            .getFederatedCredentials(ssoProfile.accountId, ssoProfile.roleName, this.accessToken)
            .map { it.roleCredentials }

    private fun retrieveDeviceToken(): DeviceToken {
        return ssoCacheManager.retrieveSSOCachedToken(ssoProfile)
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.toDeviceToken()
            ?: oidcRetrieveDeviceToken()
    }


    private fun OIDC.retrieveRegisteredClient(): RegisteredClient =
        ssoCacheManager.retrieveSSOCachedRegistration(ssoProfile)
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.toRegisteredClient(clock)
            ?: registerClient(clientName)
                .peek {
                    ssoCacheManager.storeSSOCachedRegistration(ssoProfile, it.toSSOCachedRegistration())
                }
                .onFailure { it.reason.throwIt() }

    private fun oidcRetrieveDeviceToken() =
        with(OIDC.Http(ssoProfile.region, http)) {
            retrieveRegisteredClient()
                .let { registeredClient ->
                    startDeviceAuthorization(
                        registeredClient.clientId,
                        registeredClient.clientSecret,
                        ssoProfile.startUri
                    )
                        .map { registeredClient to it }
                }
                .flatMap { (client, auth) ->
                    openBrowser(auth.verificationUriComplete)

                    var tokenResult = createToken(client.clientId, client.clientSecret, auth.deviceCode)

                    while (
                        tokenResult !is Success<DeviceToken> &&
                        clock.instant().isBefore(clock.instant().plusSeconds(auth.expiresIn))
                    ) {
                        waitFor(auth.interval * 1000)
                        tokenResult = createToken(client.clientId, client.clientSecret, auth.deviceCode)
                    }
                    tokenResult.peek {
                        ssoCacheManager.storeSSOCachedToken(
                            ssoProfile, SSOCachedToken.of(ssoProfile, it, client)
                        )
                    }
                }
        }.onFailure { it.reason.throwIt() }
}
