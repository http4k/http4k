package org.http4k.connect.amazon.cognitoidentity

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeCognitoIdentityChaosTest : FakeSystemContract(FakeCognitoIdentity()) {
    override val anyValid = Request(POST, "/")
        .header("X-Amz-Target", "AWSCognitoIdentityService.GetId")
        .body("""{"IdentityPoolId":"ldn-north-1:12345678-1234-1234-1234-123456789012"}""")
}
