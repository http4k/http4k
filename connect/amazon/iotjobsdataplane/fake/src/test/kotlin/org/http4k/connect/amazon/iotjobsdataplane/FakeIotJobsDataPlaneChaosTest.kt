package org.http4k.connect.amazon.iotjobsdataplane

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeIotJobsDataPlaneChaosTest : FakeSystemContract(FakeIotJobsDataPlane()) {
    override val anyValid = Request(GET, "/things/my-thing/jobs")
}
