package org.http4k.connect.amazon.evidently

import org.http4k.connect.amazon.FakeAwsContract

class FakeEvidentlyContract : EvidentlyContract, FakeAwsContract {
    override val http = FakeEvidently()
}
