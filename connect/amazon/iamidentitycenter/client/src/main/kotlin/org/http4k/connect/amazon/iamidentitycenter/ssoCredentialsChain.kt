package org.http4k.connect.amazon.iamidentitycenter

import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_CONFIG_FILE
import org.http4k.connect.amazon.AWS_PROFILE
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.iamidentitycenter.model.ClientName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.util.WebBrowser
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.nio.file.Path
import java.time.Clock
import kotlin.io.path.Path

fun CredentialsChain.Companion.SSO(
    env: Environment = Environment.ENV,
    profileName: ProfileName = AWS_PROFILE(env),
    configPath: Path = AWS_CONFIG_FILE(env),
    http: HttpHandler = JavaHttpClient(),
    clock: Clock = Clock.systemUTC(),
    clientName: ClientName = ClientName.of("http4k-connect-client"),
    openBrowser: (Uri) -> Any = WebBrowser()::navigateTo,
    waitFor: (Long) -> Unit = { Thread.sleep(it) },
    cachedTokenDirectory: Path = Path(System.getProperty("user.home")).resolve(".aws/sso/cache")
) = CredentialsChain(
    SSOProfile.loadProfiles(configPath)[profileName]?.let {
        CredentialsProvider.SSO(
            ssoProfile = it,
            http = http,
            clock = clock,
            clientName = clientName,
            openBrowser = openBrowser,
            waitFor = waitFor,
            cachedTokenDirectory = cachedTokenDirectory
        )
    } ?: { null },
)




