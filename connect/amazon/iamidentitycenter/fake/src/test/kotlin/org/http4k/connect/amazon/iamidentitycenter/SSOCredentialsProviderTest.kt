package org.http4k.connect.amazon.iamidentitycenter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.core.Filter
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.routing.reverseProxy
import org.junit.jupiter.api.Test

class SSOCredentialsProviderTest {

    private val ssoProfile = SSOProfile(AwsAccount.of("01234567890"), RoleName.of("hello"), Region.US_EAST_1, Uri.of("http://foobar"))
    private val clock = TestClock()

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
        val cp = CredentialsProvider.SSO(ssoProfile, clock = clock, openBrowser = {}, waitFor = {}, http = http)

        cp()
        cp()

        assertThat(count, equalTo(1))
    }
}
