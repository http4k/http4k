package org.http4k.connect.amazon.sns

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeSNSTest : SNSContract, FakeAwsContract, WithRunningFake(::FakeSNS)
