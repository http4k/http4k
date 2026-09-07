package org.http4k.connect.amazon.cognitoidentity

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test

class FakeCognitoIdentityTest : CognitoIdentityContract, FakeAwsContract {
    override val http = FakeCognitoIdentity()

    override val identityPoolId = IdentityPoolId.of("ldn-north-1:12345678-1234-1234-1234-123456789012")

    @Test
    fun `a different set of logins gets its own identity`() {
        val anonymous = cognitoIdentity.getId(identityPoolId).successValue().IdentityId
        val signedIn = cognitoIdentity.getId(identityPoolId, Logins = mapOf("provider" to "token"))
            .successValue().IdentityId

        assertThat(anonymous == signedIn, equalTo(false))
        assertThat(http.identities[signedIn.value]!!.logins, equalTo(mapOf("provider" to "token")))
    }
}
