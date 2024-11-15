package org.http4k.connect.amazon.s3

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeS3ChaosTest : FakeSystemContract(FakeS3()) {
    override val anyValid = Request(GET, "/")
}
