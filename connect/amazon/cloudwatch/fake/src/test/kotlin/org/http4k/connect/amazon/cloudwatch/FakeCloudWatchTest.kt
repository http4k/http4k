package org.http4k.connect.amazon.cloudwatch

import org.http4k.connect.amazon.FakeAwsContract

class FakeCloudWatchTest : CloudWatchContract, FakeAwsContract {
    override val http = FakeCloudWatch()
}
