package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.POST
import org.http4k.core.Request

class FakeIotDataPlaneChaosTest : FakeSystemContract(FakeIotDataPlane()) {
    override val anyValid = Request(POST, "/topics/foo")
}
