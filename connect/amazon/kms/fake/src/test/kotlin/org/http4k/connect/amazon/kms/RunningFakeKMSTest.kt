package org.http4k.connect.amazon.kms

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeKMSTest : KMSContract, FakeAwsContract, WithRunningFake(::FakeKMS)
