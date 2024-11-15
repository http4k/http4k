package org.http4k.connect.amazon.secretsmanager

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.fakeAwsEnvironment

class FakeSecretsManagerTest : SecretsManagerContract, FakeAwsContract {
    override val http = FakeSecretsManager()
    override val nameOrArn = ARN.of("arn:aws:secretsmanager:us-west-2:123456789012:secret:MYSECRET").value
}
