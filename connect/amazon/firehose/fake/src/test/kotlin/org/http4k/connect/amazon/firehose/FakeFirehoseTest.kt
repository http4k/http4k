package org.http4k.connect.amazon.firehose

import org.http4k.connect.amazon.FakeAwsContract

class FakeFirehoseTest : FirehoseContract, FakeAwsContract {
    override val http = FakeFirehose()
}
