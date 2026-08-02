package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.core.Uri

class RunningFakeIotDataPlaneTest : IotDataPlaneContract, FakeAwsContract, WithRunningFake(::FakeIotDataPlane) {
    override val endpoint = Uri.of("https://http4k-ats.iot.ldn-north-1.amazonaws.com")
}
