package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeCloudWatchLogsTest : CloudWatchLogsContract, FakeAwsContract, WithRunningFake(::FakeCloudWatchLogs)
