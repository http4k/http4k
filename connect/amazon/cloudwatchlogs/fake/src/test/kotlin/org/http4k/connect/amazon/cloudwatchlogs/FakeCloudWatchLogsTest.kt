package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.connect.amazon.FakeAwsContract

class FakeCloudWatchLogsTest : CloudWatchLogsContract, FakeAwsContract {
    override val http = FakeCloudWatchLogs()
}
