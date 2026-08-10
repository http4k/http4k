package org.http4k.connect.amazon.iot

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.iot.model.S3Location

class RunningFakeIotTest : IotContract, FakeAwsContract, WithRunningFake(::FakeIot) {
    override val thingArn = ARN.of("arn:aws:iot:ldn-north-1:000000000000:thing/my-thing")

    override val streamRoleArn = ARN.of("arn:aws:iam::000000000000:role/http4k-stream")

    override val streamS3Location = S3Location(bucket = "http4k-bucket", key = "image.bin")
}
