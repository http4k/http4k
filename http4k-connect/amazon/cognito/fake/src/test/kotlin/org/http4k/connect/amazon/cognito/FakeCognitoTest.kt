package org.http4k.connect.amazon.cognito

import org.http4k.connect.amazon.FakeAwsContract

class FakeCognitoTest : CognitoContract, FakeAwsContract {
    override val http = FakeCognito()
}
