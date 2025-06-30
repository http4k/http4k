package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeCloudWatchTest : CloudWatchContract, FakeAwsContract, WithRunningFake(::FakeCloudWatch)
