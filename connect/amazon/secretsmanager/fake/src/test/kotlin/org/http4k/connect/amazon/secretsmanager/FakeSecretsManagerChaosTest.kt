package org.http4k.connect.amazon.secretsmanager

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeSecretsManagerChaosTest : FakeSystemContract(FakeSecretsManager()) {
    override val anyValid = Request(GET, "/")
}
