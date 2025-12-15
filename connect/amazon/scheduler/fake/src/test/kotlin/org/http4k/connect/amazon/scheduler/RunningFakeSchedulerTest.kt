package org.http4k.connect.amazon.scheduler

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeSchedulerTest : SchedulerContract, FakeAwsContract, WithRunningFake(::FakeScheduler)
