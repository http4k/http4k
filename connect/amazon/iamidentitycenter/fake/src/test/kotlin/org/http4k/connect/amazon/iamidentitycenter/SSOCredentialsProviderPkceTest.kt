package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.model.SSOSession
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.query
import org.http4k.routing.reverseProxy
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

class SSOCredentialsProviderPkceTest : PortBasedTest {

    private val ssoProfile =
        SSOProfile(
            ssoSession = SSOSession.of("my-sso"),
            accountId = AwsAccount.of("01234567890"),
            roleName = RoleName.of("hello"),
            region = Region.US_EAST_1,
            startUri = Uri.of("http://foobar")
        )

    private val cachedTokenDirectory: Path = Files.createTempDirectory("cache")

    @AfterEach
    fun cleanup() {
        for (file in cachedTokenDirectory.listDirectoryEntries())
            file.deleteExisting()

        cachedTokenDirectory.deleteIfExists()
    }

    @Test
    fun `support pkce flow`() = runBlocking {

        val http: HttpHandler = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to FakeOIDC(),
        )

        class FakeCodeCatchingHttp4kServer(val authCodeCatcher: HttpHandler) : Http4kServer {
            override fun start(): Http4kServer = this
            override fun stop(): Http4kServer = this
            override fun port(): Int = 0

            fun accept(uri: Uri) {
                authCodeCatcher.invoke(Request(Method.GET, uri.query("code", "AUTH-$uri")))
            }
        }

        val serverConfig = object : ServerConfig {
            lateinit var server: FakeCodeCatchingHttp4kServer
            override fun toServer(http: HttpHandler): Http4kServer =
                FakeCodeCatchingHttp4kServer(http).also { server = it }
        }
        val openBrowser = { uri: Uri -> serverConfig.server.accept(uri) }

        val credentials = CredentialsProvider.SSO(
            ssoProfile,
            http = http,
            cachedTokenDirectory = cachedTokenDirectory,
            login = SSOLogin.enabled(
                openBrowser = openBrowser,
                waitFor = {},
                serverConfig = serverConfig
            )
        ).invoke()

        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    accessKey = "accessKeyId",
                    secretKey = "secretAccessKey",
                    sessionToken = "lluntneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }

}
