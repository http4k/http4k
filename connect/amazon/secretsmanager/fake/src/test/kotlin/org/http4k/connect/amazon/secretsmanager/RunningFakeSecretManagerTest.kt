package org.http4k.connect.amazon.secretsmanager

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN

class RunningFakeSecretManagerTest : SecretsManagerContract, FakeAwsContract, WithRunningFake(::FakeSecretsManager) {
    override val nameOrArn = ARN.of("arn:aws:secretsmanager:us-west-2:123456789012:secret:MYSECRET").value
}
