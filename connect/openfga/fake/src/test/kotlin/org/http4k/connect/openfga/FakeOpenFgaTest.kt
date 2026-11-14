package org.http4k.connect.openfga

class FakeOpenFgaTest : OpenFgaContract {
    override val http = FakeOpenFga()
}
