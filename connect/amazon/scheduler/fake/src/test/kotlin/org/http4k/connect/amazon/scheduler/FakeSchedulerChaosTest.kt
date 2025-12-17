package org.http4k.connect.amazon.scheduler

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeSchedulerChaosTest : FakeSystemContract(FakeScheduler()) {
    override val anyValid = Request(GET, "/")
}
