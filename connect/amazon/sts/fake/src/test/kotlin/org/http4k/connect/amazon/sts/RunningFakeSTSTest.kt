package org.http4k.connect.amazon.sts

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeSTSTest : STSContract, FakeAwsContract, WithRunningFake(::FakeSTS)
