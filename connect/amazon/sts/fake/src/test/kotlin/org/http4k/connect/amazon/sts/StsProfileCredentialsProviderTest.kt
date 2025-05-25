package org.http4k.connect.amazon.sts

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.core.model.ProfileName
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class StsProfileCredentialsProviderTest {

    private val clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC)

    private val credentialsFile = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText(
            """
            [default]
            aws_access_key_id = key123
            aws_secret_access_key = secret123
            
            [dev]
            role_arn = arn:aws:iam::123456789:role/special-role
            source_profile = default
            
            [dev2]
            role_arn = arn:aws:iam::987654321:role/super-special-role
            source_profile = dev
            
            [invalid]
            aws_region = us-east-1
            
            [invalidSource]
            role_arn = arn:aws:iam::123456789:role/special-role
            source_profile = invalid
        """
        )
    }

    private val configFile = Files.createTempFile("config", "ini").also {
        it.toFile().writeText(
            """
            [default]
            
            [profile prod]
            role_arn = arn:aws:iam::987654321:role/special-role
            source_profile = default
        """
        )
    }

    private fun getCredentials(name: ProfileName): AwsCredentials? {
        return CredentialsChain.StsProfile(
            credentialsPath = credentialsFile,
            configPath = configFile,
            profileName = name,
            getStsClient = { FakeSTS(clock).client() }
        ).invoke()
    }

    @AfterEach
    fun cleanup() {
        credentialsFile.toFile().delete()
        configFile.toFile().delete()
    }

    @Test
    fun `load missing profile`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("missing")),
            absent()
        )
    }

    @Test
    fun `cannot load invalid profile`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("invalid")),
            absent()
        )
    }

    @Test
    fun `load default profile - by credentials`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("default")),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `load profile - assume with credentials`() = runBlocking {
        val credentials = getCredentials(ProfileName.of("dev"))
        assertThat(credentials, present())
        assertThat(credentials!!.accessKey, equalTo("accessKeyId"))
        assertThat(credentials.secretKey, equalTo("secretAccessKey"))
        assertThat(credentials.sessionToken, present())
    }

    @Test
    fun `load profile - assume with assumed profile`() = runBlocking {
        val credentials = getCredentials(ProfileName.of("dev2"))
        assertThat(credentials, present())
        assertThat(credentials!!.accessKey, equalTo("accessKeyId"))
        assertThat(credentials.secretKey, equalTo("secretAccessKey"))
        assertThat(credentials.sessionToken, present())
    }

    @Test
    fun `load profile - cannot assume when source profile invalid`() = runBlocking {
        assertThat(
            getCredentials(ProfileName.of("invalidSource")),
            absent()
        )
    }

    @Test
    fun `load profile from config - assume with credentials`() = runBlocking {
        val credentials = getCredentials(ProfileName.of("prod"))
        assertThat(credentials, present())
        assertThat(credentials!!.accessKey, equalTo("accessKeyId"))
        assertThat(credentials.secretKey, equalTo("secretAccessKey"))
        assertThat(credentials.sessionToken, present())
    }
}
