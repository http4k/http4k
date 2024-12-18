package org.http4k.connect.amazon.iamidentitycenter

import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.onFailure
import dev.forkhandles.result4k.peek
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.AuthCode
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.PKCECodeVerifier
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.model.sessionName
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.AuthorizationStarted
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.DeviceToken
import org.http4k.connect.amazon.iamidentitycenter.oidc.action.RegisteredClient
import org.http4k.connect.amazon.iamidentitycenter.sso.action.RoleCredentials
import org.http4k.connect.util.WebBrowser
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.lens.Query
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.static
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path

/**
 * Use SSO to log into the AWS command line using a browser interaction
 */
val redirectUri = Uri.of("http://127.0.0.1/oauth/callback")

sealed interface SSOLogin {
    companion object {
        fun enabled(
            openBrowser: (Uri) -> Any = WebBrowser()::navigateTo,
            waitFor: (Long) -> Unit = { Thread.sleep(it) },
            serverConfig: ServerConfig = SunHttp(0, stopMode = StopMode.Graceful(Duration.ofSeconds(2))),
            forceRefresh: Boolean = true
        ) = SSOLoginEnabled(openBrowser, waitFor, serverConfig, forceRefresh)

        val disabled = SSOLoginDisabled
    }
}

class SSOLoginEnabled(
    val openBrowser: (Uri) -> Any,
    val waitFor: (Long) -> Unit,
    val serverConfig: ServerConfig,
    val forceRefresh: Boolean
) : SSOLogin

object SSOLoginDisabled : SSOLogin


fun CredentialsProvider.Companion.SSO(
    ssoProfile: SSOProfile,
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    clientName: ClientName = ClientName.of("http4k-connect-client"),
    cachedTokenDirectory: Path = Path(System.getProperty("user.home")).resolve(".aws/sso/cache"),
    login: SSOLogin = SSOLogin.enabled(),
) = object : CredentialsProvider {
    private val ssoCacheManager = SSOCacheManager(ssoProfile, cachedTokenDirectory, clientName)
    private val ref = AtomicReference<RoleCredentials>(null)

    override fun invoke() = with(ref.get()?.takeIf { it.expiration.toInstant().isAfter(clock.instant()) }
        ?: retrieveDeviceToken().getAwsCredentials().peek(ref::set).onFailure { it.reason.throwIt() }) {
        AwsCredentials(
            accessKeyId.value,
            secretAccessKey.value,
            sessionToken.value
        )
    }

    private fun DeviceToken.getAwsCredentials() = SSO.Http(ssoProfile.region, http)
        .getFederatedCredentials(ssoProfile.accountId, ssoProfile.roleName, this.accessToken).map { it.roleCredentials }

    private fun retrieveValidCachedDeviceToken(forceRefresh: Boolean = false): DeviceToken? =
        (if (!forceRefresh) ssoCacheManager.retrieveSSOCachedToken() else null)
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.toDeviceToken()

    private fun retrieveDeviceToken(): DeviceToken = when (login) {
        SSOLoginDisabled -> retrieveValidCachedDeviceToken()
            ?: throw Exception("Error loading SSO Token: Token for ${ssoProfile.sessionName()} does not exist")

        is SSOLoginEnabled -> retrieveValidCachedDeviceToken(forceRefresh = login.forceRefresh)
            ?: login.oidcRetrieveDeviceToken()
    }


    private fun SSOLoginEnabled.oidcRetrieveDeviceToken(): DeviceToken = with(OIDC.Http(ssoProfile.region, http)) {

        val scopes: List<String>? = if (ssoProfile.ssoSession != null) ssoProfile.ssoSessionScopes() else null
        val grantTypes = if (ssoProfile.ssoSession != null) listOf("authorization_code", "refresh_token") else null
        val redirectUris = if (ssoProfile.ssoSession != null) listOf(redirectUri) else null
        val issuerUrl = if (ssoProfile.ssoSession != null) ssoProfile.startUri else null

        val client = ssoCacheManager.retrieveSSOCachedRegistration()
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.toRegisteredClient(clock)
            ?: registerClient(clientName, scopes, grantTypes, redirectUris, issuerUrl)
                .peek {
                    ssoCacheManager.storeSSOCachedRegistration(SSOCachedRegistration.of(it, scopes, grantTypes))
                }.onFailure { it.reason.throwIt() }

        (if (ssoProfile.ssoSession == null) {
            startDeviceAuthorization(client.clientId, client.clientSecret, ssoProfile.startUri)
                .flatMap { auth ->
                    openBrowser(auth.verificationUriComplete)
                    var tokenResult = createDeviceCodeToken(client, auth)

                    while (tokenResult !is Success<DeviceToken> && clock.instant()
                            .isBefore(clock.instant().plusSeconds(auth.expiresIn))
                    ) {
                        waitFor(auth.interval * 1000)
                        tokenResult = createDeviceCodeToken(client, auth)
                    }

                    tokenResult

                }
        } else {
            createAuthCodeToken(client, startPkceAuthorization(client))
        }).peek {
            ssoCacheManager.storeSSOCachedToken(SSOCachedToken.of(ssoProfile, it, client, clock))
        }.onFailure { it.reason.throwIt() }
    }

    private fun SSOLoginEnabled.startPkceAuthorization(registeredClient: RegisteredClient): PkceAuth {
        val expectedState = UUID.randomUUID().toString()
        val (challenge, codeVerifier) = PKCES256Generator.generate()
        val authRef = AtomicReference<Auth>(null)
        val sem = Semaphore(0)
        val redirectUriWithPort =
            authCodeCatcher(authRef, sem).asServer(serverConfig)
                .use { server ->
                    server.start()
                    val redirectUriWithPort = redirectUri.port(server.port())
                    openBrowser(
                        OIDC.extractBaseUri(region = ssoProfile.region)
                            .path("authorize")
                            .query("response_type", "code")
                            .query("client_id", registeredClient.clientId.value)
                            .query("redirect_uri", redirectUriWithPort.toString())
                            .query("state", expectedState)
                            .query("code_challenge_method", "S256")
                            .query("scope", ssoProfile.ssoSessionScopes().joinToString(" "))
                            .query("code_challenge", challenge.value)
                    )

                    check(sem.tryAcquire(60L, TimeUnit.SECONDS)) {
                        "Failed to retrieve an authorization code."
                    }

                    redirectUriWithPort
                }

        val auth: Auth? = authRef.get()
        check(auth?.state == expectedState) {
            "State parameter does not match expected value."
        }

        checkNotNull(auth.code) {
            "Request denied: ${auth.error}"
        }

        return PkceAuth(codeVerifier, auth.code, redirectUriWithPort)

    }

}

private fun OIDC.createAuthCodeToken(
    client: RegisteredClient,
    auth: PkceAuth
) = createToken(
    client.clientId,
    client.clientSecret,
    "authorization_code",
    redirectUri = auth.redirectUri,
    codeVerifier = auth.codeVerifier,
    code = auth.code
)

private fun OIDC.createDeviceCodeToken(
    client: RegisteredClient,
    auth: AuthorizationStarted
) = createToken(
    client.clientId, client.clientSecret, "urn:ietf:params:oauth:grant-type:device_code", auth.deviceCode
)

private fun SSOProfile.ssoSessionScopes(): List<String> = ssoRegistrationScopes ?: listOf("sso:account:access")

fun OIDC.Companion.extractBaseUri(region: Region): Uri {
    var uri: Uri? = null

    Http(region, { r: Request -> uri = r.uri; Response(Status.NOT_IMPLEMENTED) }).invoke(object :
        OIDCAction<String>(String::class) {
        override fun toRequest(): Request = Request(Method.GET, "/ping")
    })

    return checkNotNull(uri)
}

private val authCodeCatcher: (AtomicReference<Auth>, Semaphore) -> HttpHandler = { authRef, s ->
    { r: Request ->
        val code = Query.optional("code")(r)?.let { AuthCode.of(it) }
        val state = Query.optional("state")(r)
        val error = Query.optional("error")(r)
        val errorDescription = Query.optional("error_description")(r)
        authRef.set(Auth(code, state, error, errorDescription))
        s.release()
        static(Classpath("www")).withBasePath(redirectUri.path)(r)
    }
}


data class PkceAuth(val codeVerifier: PKCECodeVerifier, val code: AuthCode, val redirectUri: Uri)

data class Auth(val code: AuthCode?, val state: String?, val error: String?, val errorDescription: String?)

