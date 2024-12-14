package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.connect.amazon.iamidentitycenter.model.SSOSession
import org.http4k.core.Uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

class SSOProfileLoadingTest {


    private val configPath = Files.createTempFile("credentials", "ini").also {
        it.toFile().writeText(
            """
                [default]
                
                [profile dev]
                sso_account_id = 01234567890
                sso_role_name = hello
                sso_region = us-east-1
                sso_start_url = http://foobar
                
                [profile prod]
                sso_session = my-sso
                sso_account_id = 01234567891
                sso_role_name = hello
                
                [sso-session my-sso]
                sso_region = us-east-1
                sso_start_url = http://bizbaz
            """.trimIndent()
        )
    }


    @AfterEach
    fun cleanup() {
        configPath.deleteIfExists()
    }

    @Test
    fun `load SSO profile`() {

        val profile = SSOProfile.loadProfiles(configPath)[ProfileName.of("dev")]

        assertThat(
            profile,
            equalTo(
                SSOProfile(
                    AwsAccount.Companion.of("01234567890"),
                    RoleName.Companion.of("hello"),
                    Region.Companion.US_EAST_1,
                    Uri.Companion.of("http://foobar"),
                )
            )
        )
    }


    @Test
    fun `load and merse SSO profile`() {

        val profile = SSOProfile.loadProfiles(configPath)[ProfileName.of("prod")]

        assertThat(
            profile,
            equalTo(
                SSOProfile(
                    AwsAccount.Companion.of("01234567891"),
                    RoleName.Companion.of("hello"),
                    Region.Companion.US_EAST_1,
                    Uri.Companion.of("http://bizbaz"),
                    SSOSession.of("my-sso")
                )
            )
        )
    }
}
