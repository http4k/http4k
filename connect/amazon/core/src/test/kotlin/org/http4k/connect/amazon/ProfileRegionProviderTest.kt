package org.http4k.connect.amazon

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.config.Environment
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.with
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.Path

class ProfileRegionProviderTest {

    private val credentialsFile = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText(
            """
            [default]
            aws_access_key_id = key123
            aws_secret_access_key = secret123
            region = ca-central-1
            
            [dev]
            aws_access_key_id = key456
            aws_secret_access_key = secret456
            region = us-east-1
            
            [staging]
            aws_access_key_id = key987
            aws_secret_access_key = secret987
            
            [prod]
            aws_access_key_id = key789
            aws_secret_access_key = secret789
        """
        )
    }

    private val configFile = Files.createTempFile("config", "ini").also {
        it.toFile().writeText(
            """
            [default]
            region = eu-west-2
            
            [profile staging]
            region = eu-west-1
        """
        )
    }

    @AfterEach
    fun cleanup() {
        credentialsFile.toFile().delete()
        configFile.toFile().delete()
    }

    private fun getRegion(name: ProfileName) = RegionProvider.Profile(name, credentialsFile, configFile).invoke()

    @Test
    fun `default profile in custom file`() = assertThat(
        getRegion(ProfileName.of("default")),
        equalTo(Region.CA_CENTRAL_1)
    )

    @Test
    fun `custom profile in custom file`() = assertThat(
        getRegion(ProfileName.of("dev")),
        equalTo(Region.US_EAST_1)
    )

    @Test
    fun `custom profile has no region`() = assertThat(
        getRegion(ProfileName.of("prod")),
        absent()
    )


    @Test
    fun `custom profile region in config file`() = assertThat(
        getRegion(ProfileName.of("staging")),
        equalTo(Region.EU_WEST_1)
    )

    @Test
    fun `default profile region in config file`() {
        credentialsFile.toFile().delete()
        assertThat(
            getRegion(ProfileName.of("default")),
            equalTo(Region.EU_WEST_2)
        )
    }

    @Test
    fun `missing profile`() = assertThat(
        getRegion(ProfileName.of("missing")),
        absent()
    )

    @Test
    fun `missing file`() = assertThat(
        RegionProvider.Profile(
            Environment.EMPTY.with(
                AWS_CREDENTIAL_PROFILES_FILE of Path("foobar"),
                AWS_CONFIG_FILE of Path("raboof"),
            )
        )(),
        absent()
    )
}
