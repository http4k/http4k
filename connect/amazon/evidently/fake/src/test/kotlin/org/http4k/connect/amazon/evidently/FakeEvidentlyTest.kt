package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.FakeAwsContract

class FakeEvidentlyTest : EvidentlyContract, FakeAwsContract {
    override val http = FakeEvidently()
}
