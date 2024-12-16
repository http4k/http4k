package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.model.cachedRegistrationPath
import org.http4k.connect.amazon.iamidentitycenter.model.cachedTokenPath
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.routing.reverseProxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

class SSOCredentialsProviderTest {

    private val ssoProfile =
        SSOProfile(AwsAccount.of("01234567890"), RoleName.of("hello"), Region.US_EAST_1, Uri.of("http://foobar"))

    val cachedTokenDirectory: Path = Files.createTempDirectory("cache")

    private val clock = TestClock()

    @AfterEach
    fun cleanup() {
        for (file in cachedTokenDirectory.listDirectoryEntries())
            file.deleteExisting()

        cachedTokenDirectory.deleteIfExists()
    }

    @Test
    fun `takes account of expiry date`() {
        var count = 0

        val sso = Filter { next ->
            {
                count++
                next(it)
            }
        }.then(FakeSSO(clock))
        val http = reverseProxy(
            "sso" to sso,
            "oidc" to FakeOIDC(clock),
        )
        val cp = CredentialsProvider.SSO(
            ssoProfile,
            clock = clock,
            openBrowser = {},
            waitFor = {},
            http = http,
            cachedTokenDirectory = cachedTokenDirectory
        )

        cp()
        cp()

        assertThat(count, equalTo(1))
    }


    @Test
    fun `use cached sso credentials`() {
        ssoProfile.cachedTokenPath(cachedTokenDirectory).toFile().writeText(
            """
                {
                  "startUrl": "http://foobar",
                  "region": "us-east-1",
                  "accessToken": "AccessToken-http4k-connect-client",
                  "expiresAt": "2024-12-15T10:59:59Z",
                  "clientId": "http4k-connect-client",
                  "clientSecret": "http4k-connect-client",
                  "registrationExpiresAt": "2025-12-15T10:59:59Z"
                }
                """.trimIndent()
        )
        var count = 0

        val oidc = Filter { next ->
            {
                count++
                next(it)
            }
        }.then(FakeOIDC())

        val http: HttpHandler = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to oidc,
        )

        val credentials = CredentialsProvider.SSO(
            ssoProfile,
            openBrowser = {}, waitFor = {},
            http = http,
            clock = clock,
            cachedTokenDirectory = cachedTokenDirectory
        ).invoke()

        assertThat(count, equalTo(0))
        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    "accessKeyId",
                    "secretAccessKey",
                    "tneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }


    @Test
    fun `use oidc credentials if access token not cached`() {
        var count = 0

        val oidc = Filter { next ->
            {
                count++
                next(it)
            }
        }.then(FakeOIDC())

        val http: HttpHandler = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to oidc,
        )

        val credentials = CredentialsProvider.SSO(
            ssoProfile,
            openBrowser = {}, waitFor = {},
            http = http,
            cachedTokenDirectory = cachedTokenDirectory
        ).invoke()

        assertThat(count, equalTo(3))
        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    accessKey = "accessKeyId",
                    secretKey = "secretAccessKey",
                    sessionToken = "raboof//:ptthtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-ECIVEDtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }

    @Test
    fun `use previously registered client if access token expired`() {
        var count = 0

        val oidc = Filter { next ->
            {
                count++
                next(it)
            }
        }.then(FakeOIDC())

        val http: HttpHandler = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to oidc,
        )

        ssoProfile.cachedRegistrationPath(cachedTokenDirectory).toFile().writeText(
            """
                {
                  "clientId": "http4k-connect-client",
                  "clientSecret": "http4k-connect-client",
                  "expiresAt": "2025-01-15T10:59:59Z"
                }
                """.trimIndent()
        )
        val credentials = CredentialsProvider.SSO(
            SSOProfile(
                AwsAccount.Companion.of("01234567890"),
                RoleName.Companion.of("hello"),
                Region.Companion.US_EAST_1,
                Uri.Companion.of("http://foobar"),
            ),
            openBrowser = {}, waitFor = {},
            http = http,
            cachedTokenDirectory = cachedTokenDirectory
        ).invoke()

        assertThat(count, equalTo(2))
        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    accessKey = "accessKeyId",
                    secretKey = "secretAccessKey",
                    sessionToken = "raboof//:ptthtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-ECIVEDtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }

    @Test
    fun `register new client if access token and registration expired`() {
        var count = 0

        val oidc = Filter { next ->
            {
                count++
                next(it)
            }
        }.then(FakeOIDC())

        val http: HttpHandler = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to oidc,
        )

        ssoProfile.cachedTokenPath(cachedTokenDirectory).toFile().writeText(
            """
                {
                  "startUrl": "http://foobar",
                  "region": "us-east-1",
                  "accessToken": "AccessToken-http4k-connect-client",
                  "expiresAt": "2023-01-15T10:59:59Z",
                  "clientId": "http4k-connect-client",
                  "clientSecret": "http4k-connect-client",
                  "registrationExpiresAt": "2024-01-15T10:59:59Z"
                }
                """.trimIndent()
        )
        val credentials = CredentialsProvider.SSO(
            SSOProfile(
                AwsAccount.Companion.of("01234567890"),
                RoleName.Companion.of("hello"),
                Region.Companion.US_EAST_1,
                Uri.Companion.of("http://foobar"),
            ),
            openBrowser = {}, waitFor = {},
            http = http,
            cachedTokenDirectory = cachedTokenDirectory
        ).invoke()

        assertThat(count, equalTo(3))
        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    accessKey = "accessKeyId",
                    secretKey = "secretAccessKey",
                    sessionToken = "raboof//:ptthtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-ECIVEDtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }
}
