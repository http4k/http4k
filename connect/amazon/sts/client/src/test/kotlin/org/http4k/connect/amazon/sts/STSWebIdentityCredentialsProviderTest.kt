package org.http4k.connect.amazon.sts

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.EMPTY
import org.http4k.connect.TestClock
import org.http4k.connect.amazon.AWS_REGION
import org.http4k.connect.amazon.AWS_ROLE_ARN
import org.http4k.connect.amazon.AWS_WEB_IDENTITY_TOKEN
import org.http4k.connect.amazon.AWS_WEB_IDENTITY_TOKEN_FILE
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.core.model.Expiration
import org.http4k.connect.amazon.core.model.Region.Companion.US_EAST_1
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class STSWebIdentityCredentialsProviderTest {

    private val http = mockk<HttpHandler>()
    private val now = Instant.now()
    private val clock = TestClock(now)

    @Test
    fun `gets credentials using file`() {
        checkCanAssumeRole(
            EMPTY
                .with(
                    AWS_REGION of US_EAST_1,
                    AWS_ROLE_ARN of ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole"),
                    AWS_WEB_IDENTITY_TOKEN_FILE of File(javaClass.getResource("/webidentitytoken.txt").file)
                )
        )
    }

    @Test
    fun `gets credentials using hardcoded token`() {
        checkCanAssumeRole(
            EMPTY
                .with(
                    AWS_REGION of US_EAST_1,
                    AWS_ROLE_ARN of ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole"),
                    AWS_WEB_IDENTITY_TOKEN of WebIdentityToken.of("foobar")
                )
        )
    }

    private fun checkCanAssumeRole(env: Environment) {
        val provider = CredentialsProvider.STSWebIdentity(
            env, http, clock, Duration.ofSeconds(60)
        )

        val firstCreds = credentialsExpiringAt(now.plusSeconds(61), 1)
        every { http(any()) } returns Response(OK)
            .body(
                """<AssumeRoleWithWebIdentityResponse xmlns="https://sts.amazonaws.comdoc/2011-06-15/">
    <AssumeRoleWithWebIdentityResult>
        <AssumedRoleUser>
            <Arn>arn:aws:iam::account:role/role-name-with-path</Arn>
            <AssumedRoleId>roleIdForThisRole</AssumedRoleId>
        </AssumedRoleUser>
        <Credentials>
            <AccessKeyId>1</AccessKeyId>
            <SecretAccessKey>SecretAccessKey</SecretAccessKey>
            <SessionToken>
                SessionToken
            </SessionToken>
            <Expiration>2021-08-27T18:26:38.152523Z</Expiration>
        </Credentials>
        <SubjectFromWebIdentityToken>amzn1.account.111111111111111111111111</SubjectFromWebIdentityToken>
        <Audience>client.111111111111111111111111.1118@apps.example.com</Audience>
        <SourceIdentity>SourceIdentityValue</SourceIdentity>
        <Provider>www.amazon.com</Provider>
    </AssumeRoleWithWebIdentityResult>
    <ResponseMetadata>
        <RequestId>11111111-1111-1111-1111-111111111111</RequestId>
    </ResponseMetadata>
</AssumeRoleWithWebIdentityResponse>
"""
            )

        assertThat(provider(), equalTo(firstCreds.asHttp4k()))

        verify(exactly = 1) { http.invoke(any()) }
    }

    private fun credentialsExpiringAt(expiry: Instant, counter: Int) = Credentials(
        SessionToken.of("SessionToken"),
        AccessKeyId.of(counter.toString()),
        SecretAccessKey.of("SecretAccessKey"),
        Expiration.of(ZonedDateTime.ofInstant(expiry, ZoneId.of("UTC"))),
        ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole")
    )
}
