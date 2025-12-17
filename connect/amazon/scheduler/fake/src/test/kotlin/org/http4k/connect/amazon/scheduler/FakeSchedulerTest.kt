package org.http4k.connect.amazon.scheduler

import org.http4k.connect.amazon.FakeAwsContract

class FakeSchedulerTest : SchedulerContract, FakeAwsContract {
    override val http = FakeScheduler()
}
