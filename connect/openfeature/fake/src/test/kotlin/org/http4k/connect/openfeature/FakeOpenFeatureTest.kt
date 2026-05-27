package org.http4k.connect.openfeature

import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey

class FakeOpenFeatureTest : OpenFeatureContract {
    private val fake = FakeOpenFeature()
    override val http = fake

    override fun seed(key: FlagKey, value: Any?) {
        fake[key] = value
    }

    override fun seedRule(key: FlagKey, value: Any?, predicate: (EvaluationContext) -> Boolean) {
        fake.rule(key, predicate) returns value
    }
}
