package org.http4k.connect.amazon.systemsmanager

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeSystemsManagerTest : SystemsManagerContract, FakeAwsContract, WithRunningFake(::FakeSystemsManager)
