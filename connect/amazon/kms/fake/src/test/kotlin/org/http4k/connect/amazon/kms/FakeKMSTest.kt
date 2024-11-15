package org.http4k.connect.amazon.kms

import org.http4k.connect.amazon.FakeAwsContract

class FakeKMSTest : KMSContract, FakeAwsContract {
    override val http = FakeKMS()
}
