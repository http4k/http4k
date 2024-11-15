package org.http4k.connect.amazon.systemsmanager

import org.http4k.connect.amazon.FakeAwsContract

class FakeSystemsManagerTest : SystemsManagerContract, FakeAwsContract {
    override val http = FakeSystemsManager()
}
