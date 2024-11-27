package org.http4k.connect.amazon.eventbridge

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeEventBridgeTest : EventBridgeContract, FakeAwsContract, WithRunningFake(::FakeEventBridge)
