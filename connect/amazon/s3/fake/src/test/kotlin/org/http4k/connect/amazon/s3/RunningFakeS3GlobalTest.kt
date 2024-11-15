package org.http4k.connect.amazon.s3

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeS3GlobalTest : S3GlobalContract, FakeAwsContract, WithRunningFake(::FakeS3)
