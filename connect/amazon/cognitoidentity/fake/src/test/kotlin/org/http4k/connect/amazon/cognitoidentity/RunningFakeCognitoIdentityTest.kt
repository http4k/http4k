package org.http4k.connect.amazon.cognitoidentity

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.cognitoidentity.model.IdentityPoolId

class RunningFakeCognitoIdentityTest : CognitoIdentityContract, FakeAwsContract,
    WithRunningFake(::FakeCognitoIdentity) {
    override val identityPoolId = IdentityPoolId.of("ldn-north-1:12345678-1234-1234-1234-123456789012")
}
