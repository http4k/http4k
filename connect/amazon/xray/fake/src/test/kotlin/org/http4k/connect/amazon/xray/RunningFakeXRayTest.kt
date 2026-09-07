package org.http4k.connect.amazon.xray

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeXRayTest : XRayContract, FakeAwsContract, WithRunningFake(::FakeXRay)
