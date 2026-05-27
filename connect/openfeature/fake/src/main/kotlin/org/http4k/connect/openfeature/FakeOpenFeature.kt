package org.http4k.connect.openfeature

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.openfeature.endpoints.evaluateAllFlags
import org.http4k.connect.openfeature.endpoints.evaluateFlag
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.Reason
import org.http4k.connect.openfeature.model.Reason.STATIC
import org.http4k.connect.openfeature.model.Reason.TARGETING_MATCH
import org.http4k.core.Uri
import org.http4k.routing.routes

data class FlagRule(
    val key: FlagKey,
    val value: Any?,
    val matches: (EvaluationContext) -> Boolean = { true },
    val reason: Reason = TARGETING_MATCH
)

class FakeOpenFeature(
    val rules: MutableList<FlagRule> = mutableListOf()
) : ChaoticHttpHandler() {

    fun client() = OpenFeature.Http(Uri.of("http://localhost"), { OpenFeatureToken.of("test") }, this)

    operator fun set(key: FlagKey, value: Any?) {
        rules += FlagRule(key, value, { true }, STATIC)
    }

    fun rule(key: FlagKey, predicate: (EvaluationContext) -> Boolean) = RuleBuilder(key, predicate)

    inner class RuleBuilder internal constructor(
        private val key: FlagKey,
        private val predicate: (EvaluationContext) -> Boolean
    ) {
        infix fun returns(value: Any?) {
            rules += FlagRule(key, value, predicate)
        }
    }

    override val app = routes(
        evaluateFlag(rules),
        evaluateAllFlags(rules)
    )
}

// port 43778
fun main() {
    FakeOpenFeature().start()
}
