package org.http4k.connect.amazon.cloudwatchlogs

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeCloudWatchLogsChaosTest : FakeSystemContract(FakeCloudWatchLogs()) {
    override val anyValid = Request(GET, "/")
}
