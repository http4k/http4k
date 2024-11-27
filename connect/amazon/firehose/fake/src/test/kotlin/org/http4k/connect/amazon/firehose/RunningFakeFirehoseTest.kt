package org.http4k.connect.amazon.firehose

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeFirehoseTest : FirehoseContract, FakeAwsContract, WithRunningFake(::FakeFirehose)
