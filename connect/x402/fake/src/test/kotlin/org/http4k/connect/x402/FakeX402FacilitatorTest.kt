package org.http4k.connect.x402

class FakeX402FacilitatorTest : X402FacilitatorContract {
    override val http = FakeX402Facilitator()
}
