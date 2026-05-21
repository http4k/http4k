package org.http4k.connect.amazon.sts

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import java.time.Clock

class RunningFakeSTSTest : STSContract, FakeAwsContract, WithRunningFake(::FakeSTS) {
    override val clock: Clock = Clock.systemUTC()
}
