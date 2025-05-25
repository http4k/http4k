package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.nio.file.Files
import kotlin.io.path.Path

class ProfileCredentialsProviderTest {

    private val credentialsFile = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText(
            """
            [default]
            aws_access_key_id = key123
            aws_secret_access_key = secret123
            
            [dev]
            aws_access_key_id = key456
            aws_secret_access_key = secret456
        """
        )
    }

    private val configFile = Files.createTempFile("config", "ini").also {
        it.toFile().writeText(
            """
            [profile dev1]
            aws_access_key_id = key789
            aws_secret_access_key = secret789
        """
        )
    }

    private val env =
        Environment.EMPTY.with(AWS_CREDENTIAL_PROFILES_FILE of credentialsFile, AWS_CONFIG_FILE of configFile)

    @AfterEach
    fun cleanup() {
        credentialsFile.toFile().delete()
        configFile.toFile().delete()
    }

    private fun getCredentials(name: ProfileName): AwsCredentials? = CredentialsChain.Profile(
        profileName = name,
        credentialsPath = credentialsFile,
        configPath = configFile
    ).invoke()

    @Test
    fun `default profile in custom file`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("default")),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `custom profile in custom file`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("dev")),
            equalTo(AwsCredentials("key456", "secret456"))
        )
    }

    @Test
    fun `missing profile`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("missing")),
            absent()
        )
    }

    @Test
    fun `missing file`() = runBlocking {
        assertThat(
            CredentialsChain.Profile(Environment.EMPTY.with(AWS_CREDENTIAL_PROFILES_FILE of Path("foobar")))(),
            absent()
        )
    }

    @Test
    fun `credentials are cached`() = runBlocking {
        val expected = AwsCredentials("key123", "secret123")
        val chain = CredentialsChain.Profile(
            profileName = ProfileName.of("default"),
            credentialsPath = credentialsFile,
            configPath = configFile
        )

        assertThat(chain.invoke(), equalTo(expected))

        credentialsFile.toFile().writeText(
            """
            [default]
            aws_access_key_id = key1456
            aws_secret_access_key = secret456
        """
        )

        assertThat(chain.invoke(), equalTo(expected))
    }

    @Test
    fun `CredentialsProvider provides default credentials from env`() = runBlocking {
        assertThat(
            CredentialsProvider.Profile(env).invoke(),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `CredentialsProvider provides custom credentials from env`() = runBlocking {
        assertThat(
            CredentialsProvider.Profile(env, profileName = ProfileName.of("dev")).invoke(),
            equalTo(AwsCredentials("key456", "secret456"))
        )
    }

    @Test
    fun `CredentialsProvider provides custom config credentials from env`() = runBlocking {
        assertThat(
            CredentialsProvider.Profile(env, profileName = ProfileName.of("dev1")).invoke(),
            equalTo(AwsCredentials("key789", "secret789"))
        )
    }

    @Test
    fun `CredentialsProvider provides custom credentials from custom file`() = runBlocking {
        assertThat(
            CredentialsProvider.Profile(
                profileName = ProfileName.of("dev"),
                credentialsPath = credentialsFile
            ).invoke(),
            equalTo(AwsCredentials("key456", "secret456"))
        )
    }

    @Test
    fun `CredentialsProvider provides default credentials from custom file`() = runBlocking {
        assertThat(
            CredentialsProvider.Profile(credentialsPath = credentialsFile).invoke(),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `CredentialsProvider throws exception for missing profile`() = runBlocking {
        val ex = assertThrows<IllegalArgumentException> {
            CredentialsProvider.Profile(profileName = ProfileName.of("missing")).invoke()
        }
        assertThat(ex.message, equalTo("Could not find any valid credentials in the chain"))
    }

    @Test
    fun `CredentialsProvider throws exception for missing credentials path`() = runBlocking {
        val ex = assertThrows<IllegalArgumentException> {
            CredentialsProvider.Profile(credentialsPath = Path("foobar")).invoke()
        }
        assertThat(ex.message, equalTo("Could not find any valid credentials in the chain"))
    }
}
