package org.http4k.ai.llm.decide

import org.http4k.ai.model.ApiKey
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey
import org.http4k.connect.typesafe.JevModels
import org.http4k.filter.debug
import org.http4k.lens.value
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Assumptions.assumeTrue

class RealTypeSafeDecideTest : DecideContract, PortBasedTest {

    val token = EnvironmentKey.value(ApiKey).optional("TYPESAFE_API_KEY")

    init {
        assumeTrue(token(ENV) != null, "No API Key set - skipping")
    }

    override val decide = Decide.TypeSafe(token(ENV)!!, JavaHttpClient().debug())
    override val model = JevModels.JevLatest
}
