package org.http4k.connect.amazon.cognitoidentity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.FakeAwsEnvironment
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.model.Timestamp
import org.http4k.connect.successValue
import org.http4k.core.Method.POST
import org.http4k.core.MockHttp
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasMethod
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class HttpCognitoIdentityTest {

    private val identityPoolId = IdentityPoolId.of("us-east-1:12345678-1234-1234-1234-123456789012")
    private val identityId = IdentityId.of("us-east-1:87654321-4321-4321-4321-210987654321")

    private fun clientFor(http: MockHttp) =
        CognitoIdentity.Http(Region.US_EAST_1, CredentialsProvider.FakeAwsEnvironment(), http)

    @Test
    fun `get id builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body("""{"IdentityId":"${identityId.value}"}"""))

        val identity = clientFor(mock).getId(identityPoolId, Logins = mapOf("provider" to "token")).successValue()

        assertThat(mock.request!!, hasMethod(POST))
        assertThat(mock.request!!, hasHeader("X-Amz-Target", "AWSCognitoIdentityService.GetId"))
        assertThat(
            mock.request!!,
            hasBody("""{"IdentityPoolId":"${identityPoolId.value}","Logins":{"provider":"token"}}""")
        )
        assertThat(identity.IdentityId, equalTo(identityId))
    }

    @Test
    fun `get credentials for identity builds the documented request and unmarshals the response`() {
        val mock = MockHttp(Response(OK).body(CREDENTIALS))

        val credentials = clientFor(mock).getCredentialsForIdentity(identityId).successValue()

        assertThat(mock.request!!, hasHeader("X-Amz-Target", "AWSCognitoIdentityService.GetCredentialsForIdentity"))
        assertThat(mock.request!!, hasBody("""{"IdentityId":"${identityId.value}"}"""))
        assertThat(credentials.IdentityId, equalTo(identityId))
        assertThat(credentials.Credentials.AccessKeyId.value, equalTo("ASIAEXAMPLE"))
        assertThat(credentials.Credentials.Expiration, equalTo(Timestamp.of(1614355593)))
        assertThat(credentials.Credentials.asHttp4k().sessionToken, equalTo("token"))
    }

    @Test
    fun `requests are signed against the cognito-identity endpoint`() {
        val mock = MockHttp(Response(OK).body("""{"IdentityId":"${identityId.value}"}"""))

        clientFor(mock).getId(identityPoolId).successValue()

        assertThat(mock.request!!.uri.host, equalTo("cognito-identity.us-east-1.amazonaws.com"))
    }

    @Test
    fun `identity ids and pool ids outside the AWS format are rejected`() {
        listOf("", "no-region", "us-east-1:", ":12345678")
            .forEach {
                assertThrows<IllegalArgumentException>(it) { IdentityId.of(it) }
                assertThrows<IllegalArgumentException>(it) { IdentityPoolId.of(it) }
            }
    }
}

private val CREDENTIALS =
    """{"IdentityId":"us-east-1:87654321-4321-4321-4321-210987654321","Credentials":""" +
        """{"AccessKeyId":"ASIAEXAMPLE","SecretKey":"secret","SessionToken":"token","Expiration":1614355593}}"""
