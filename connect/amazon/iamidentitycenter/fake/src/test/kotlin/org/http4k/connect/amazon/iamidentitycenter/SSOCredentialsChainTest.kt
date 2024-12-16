package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.connect.amazon.AWS_CONFIG_FILE
import org.http4k.connect.amazon.AWS_PROFILE
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.core.with
import org.http4k.routing.reverseProxy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

class SSOCredentialsChainTest {

    val cachedTokenDirectory: Path = Files.createTempDirectory("cache")

    private val profileFile = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText(
            """
            [profile dev]
            sso_start_url = http://foobar
            sso_region = eu-west-1
            sso_account_id = 01234567890
            sso_role_name = hello
            region = us-east-1
        """
        )
    }

    private val env = Environment.EMPTY.with(AWS_CONFIG_FILE of profileFile, AWS_PROFILE of ProfileName.of("prod"))

    @AfterEach
    fun cleanup() {
        profileFile.deleteIfExists()
        for (file in cachedTokenDirectory.listDirectoryEntries())
            file.deleteExisting()

        cachedTokenDirectory.deleteIfExists()
    }

    @Test
    fun `should not provide credentials for unknown profile`() {
        val http = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to FakeOIDC(),
        )
        val credentials = CredentialsChain.SSO(
            env,
            http = http,
            openBrowser = {},
            waitFor = {},
            cachedTokenDirectory = cachedTokenDirectory
        )()


        assertThat(credentials, absent())
    }


    @Test
    fun `should use config profile profile`() {
        val http = reverseProxy(
            "sso" to FakeSSO(),
            "oidc" to FakeOIDC(),
        )
        val credentials = CredentialsChain.SSO(
            env.with(AWS_PROFILE of ProfileName.of("dev")),
            http = http,
            openBrowser = {},
            waitFor = {},
            cachedTokenDirectory = cachedTokenDirectory
        )()


        assertThat(
            credentials, equalTo(
                AwsCredentials(
                    accessKey = "accessKeyId",
                    secretKey = "secretAccessKey",
                    sessionToken = "raboof//:ptthtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-ECIVEDtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }
}

