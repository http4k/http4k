package org.http4k.connect.openfeature

import org.http4k.connect.WithRunningFake
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.Reason.STATIC
import org.junit.jupiter.api.BeforeEach
import java.util.concurrent.CopyOnWriteArrayList

class RunningFakeOpenFeatureTest : OpenFeatureContract, WithRunningFake(::makeFake) {

    @BeforeEach
    fun resetFlags() {
        rules.clear()
    }

    override fun seed(key: FlagKey, value: Any?) {
        rules += FlagRule(key, value, { true }, STATIC)
    }

    override fun seedRule(key: FlagKey, value: Any?, predicate: (EvaluationContext) -> Boolean) {
        rules += FlagRule(key, value, predicate)
    }

    companion object {
        private val rules = CopyOnWriteArrayList<FlagRule>()
        private fun makeFake() = FakeOpenFeature(rules)
    }
}
