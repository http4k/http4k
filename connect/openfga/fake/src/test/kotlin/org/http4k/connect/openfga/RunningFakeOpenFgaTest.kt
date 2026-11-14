package org.http4k.connect.openfga

import org.http4k.connect.WithRunningFake

class RunningFakeOpenFgaTest : OpenFgaContract, WithRunningFake(::FakeOpenFga)
