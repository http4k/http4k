package org.http4k.connect.amazon.sqs

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeSQSTest : SQSContract, FakeAwsContract, WithRunningFake(::FakeSQS)
