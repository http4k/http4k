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

    private val profileFile = Files.createTempFile("credentials", "ini").also {
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

    private fun getCredentials(name: ProfileName): AwsCredentials? {
        return CredentialsChain.StsProfile(
            credentialsPath = profileFile,
            profileName = name,
            getStsClient = { FakeSTS(clock).client() }
        ).invoke()
    }

    @AfterEach
    fun cleanup() {
        profileFile.toFile().delete()
    }

    @Test
    fun `load missing profile`() {
        assertThat(
            getCredentials(ProfileName.of("missing")),
            absent()
        )
    }

    @Test
    fun `cannot load invalid profile`() {
        assertThat(
            getCredentials(ProfileName.of("invalid")),
            absent()
        )
    }

    @Test
    fun `load default profile - by credentials`() {
        assertThat(
            getCredentials(ProfileName.of("default")),
            equalTo(AwsCredentials("key123", "secret123"))
        )
    }

    @Test
    fun `load profile - assume with credentials`() {
        val credentials = getCredentials(ProfileName.of("dev"))
        assertThat(credentials, present())
        assertThat(credentials!!.accessKey, equalTo("accessKeyId"))
        assertThat(credentials.secretKey, equalTo("secretAccessKey"))
        assertThat(credentials.sessionToken, present())
    }

    @Test
    fun `load profile - assume with assumed profile`() {
        val credentials = getCredentials(ProfileName.of("dev2"))
        assertThat(credentials, present())
        assertThat(credentials!!.accessKey, equalTo("accessKeyId"))
        assertThat(credentials.secretKey, equalTo("secretAccessKey"))
        assertThat(credentials.sessionToken, present())
    }

    @Test
    fun `load profile - cannot assume when source profile invalid`() {
        assertThat(
            getCredentials(ProfileName.of("invalidSource")),
            absent()
        )
    }
}
