package org.http4k.ai.llm.decide

import org.http4k.ai.model.ApiKey
import org.http4k.connect.typesafe.FakeTypeSafe
import org.http4k.connect.typesafe.JevModels

class FakeTypeSafeDecideTest : DecideContract {
    override val decide = Decide.TypeSafe(ApiKey.of("fake"), FakeTypeSafe())
    override val model = JevModels.JevLatest
}
