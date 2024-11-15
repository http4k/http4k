package org.http4k.connect.amazon.eventbridge

import org.http4k.connect.amazon.FakeAwsContract

class FakeEventBridgeTest : EventBridgeContract, FakeAwsContract {
    override val http = FakeEventBridge()
}
