package org.http4k.connect.amazon.sts.action

import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.sts.Http
import org.http4k.connect.amazon.sts.STS
import org.http4k.connect.amazon.sts.getCallerIdentity
import org.http4k.connect.successValue
import org.http4k.core.Method.POST
import org.http4k.core.MockHttp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.body.form
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetCallerIdentityActionTest {
    @Test
    fun `uses post form request and parses response`() {
        val responseXml = """
            <GetCallerIdentityResponse xmlns="https://sts.amazonaws.com/doc/2011-06-15/">
              <GetCallerIdentityResult>
                <Arn>arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name</Arn>
                <UserId>ARO123EXAMPLE123:my-role-session-name</UserId>
                <Account>123456789012</Account>
              </GetCallerIdentityResult>
              <ResponseMetadata>
                <RequestId>01234567-89ab-cdef-0123-456789abcdef</RequestId>
              </ResponseMetadata>
            </GetCallerIdentityResponse>
        """.trimIndent()

        val mockHttp = MockHttp(Response(OK).body(responseXml))
        val sts = STS.Http(
            Region.SA_EAST_1,
            CredentialsProvider.FakeAwsEnvironment(),
            mockHttp
        )

        val identity = sts.getCallerIdentity().successValue()

        assertEquals("ARO123EXAMPLE123:my-role-session-name", identity.UserId)
        assertEquals(AwsAccount.of("123456789012"), identity.Account)
        assertEquals(ARN.of("arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name"), identity.Arn)
        assertEquals(POST, mockHttp.request!!.method)
        assertEquals("GetCallerIdentity", mockHttp.request!!.form("Action"))
        assertEquals("2011-06-15", mockHttp.request!!.form("Version"))
    }
}
