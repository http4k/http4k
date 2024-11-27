package org.http4k.connect.amazon.cloudfront

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract

class RunningFakeCloudFrontTest : CloudFrontContract, FakeAwsContract, WithRunningFake(::FakeCloudFront)
