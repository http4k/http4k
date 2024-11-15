package org.http4k.connect.amazon.apprunner

import org.http4k.connect.amazon.FakeAwsContract

class FakeAppRunnerTest : AppRunnerContract, FakeAwsContract {
    override val http = FakeAppRunner()
}
