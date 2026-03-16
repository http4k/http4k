package org.http4k.connect.x402

import org.http4k.connect.WithRunningFake

class RunningFakeX402FacilitatorTest : X402FacilitatorContract, WithRunningFake(::FakeX402Facilitator)
