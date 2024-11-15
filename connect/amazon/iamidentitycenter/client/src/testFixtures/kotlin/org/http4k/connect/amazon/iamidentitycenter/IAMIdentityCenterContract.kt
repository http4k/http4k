package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.aws.AwsCredentials
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

interface IAMIdentityCenterContract : AwsContract {
    @Test
    fun `log in via SSO`() {
        val credentials = CredentialsProvider.SSO(
            SSOProfile(
                AwsAccount.of("01234567890"),
                RoleName.of("hello"),
                Region.US_EAST_1,
                Uri.of("http://foobar"),
            ),
            http,
            openBrowser = {
                assertThat(it, equalTo(Uri.of("https://device.sso.ldn-north-1.amazonaws.com/?user_code=HTTP-4KOK")))
            },
            waitFor = {}
        )()

        assertThat(
            credentials,
            equalTo(
                AwsCredentials(
                    "accessKeyId",
                    "secretAccessKey",
                    "raboof//:ptthtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-ECIVEDtneilc-tcennoc-k4ptthtneilc-tcennoc-k4ptth-nekoTsseccA"
                )
            )
        )
    }
}
