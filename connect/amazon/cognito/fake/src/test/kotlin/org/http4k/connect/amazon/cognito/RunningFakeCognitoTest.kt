package org.http4k.connect.amazon.cognito

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeCognitoTest : CognitoContract, FakeAwsContract, WithRunningFake(::FakeCognito) {
    override fun `can get access token using auth code grant`() {
        super.`can get access token using auth code grant`()
    }
}
