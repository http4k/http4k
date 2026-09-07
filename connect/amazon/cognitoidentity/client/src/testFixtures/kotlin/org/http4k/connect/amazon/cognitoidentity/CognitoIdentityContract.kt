package org.http4k.connect.amazon.cognitoidentity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.cognitoidentity.model.IdentityId
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.failureValue
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

interface CognitoIdentityContract : AwsContract {
    val identityPoolId: IdentityPoolId

    val cognitoIdentity get() = CognitoIdentity.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `get an id then exchange it for credentials`() {
        val identityId = cognitoIdentity.getId(identityPoolId).successValue().IdentityId

        val credentials = cognitoIdentity.getCredentialsForIdentity(identityId).successValue()

        assertThat(credentials.IdentityId, equalTo(identityId))
        assertThat(credentials.Credentials.AccessKeyId.value, present())
        assertThat(credentials.Credentials.asHttp4k().sessionToken, present())
    }

    @Test
    fun `getting an id twice for the same pool returns the same identity`() {
        assertThat(
            cognitoIdentity.getId(identityPoolId).successValue().IdentityId,
            equalTo(cognitoIdentity.getId(identityPoolId).successValue().IdentityId)
        )
    }

    @Test
    fun `credentials for an identity which does not exist`() {
        val unknown = IdentityId.of("${aws.region}:00000000-0000-0000-0000-000000000000")

        assertThat(cognitoIdentity.getCredentialsForIdentity(unknown).failureValue().status.successful, equalTo(false))
    }
}
