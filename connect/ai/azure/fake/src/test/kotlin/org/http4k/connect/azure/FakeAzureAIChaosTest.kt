package org.http4k.connect.azure

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method.GET
import org.http4k.core.Request

class FakeAzureAIChaosTest : FakeSystemContract(FakeAzureAI()) {
    override val anyValid = Request(GET, "/v1/models")
}
