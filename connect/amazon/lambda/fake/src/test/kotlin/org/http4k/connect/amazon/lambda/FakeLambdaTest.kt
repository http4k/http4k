package org.http4k.connect.amazon.lambda

import org.http4k.connect.amazon.FakeAwsContract

class FakeLambdaTest : LambdaContract, FakeAwsContract {
    override val http = FakeLambda(functions)
}
