package org.http4k.connect.openfeature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.openfeature.action.EvaluateAllFlags
import org.http4k.connect.openfeature.action.EvaluateFlag
import org.http4k.connect.openfeature.model.EvaluationContext
import org.http4k.connect.openfeature.model.FlagKey
import org.http4k.connect.openfeature.model.Reason.STATIC
import org.http4k.connect.openfeature.model.Reason.TARGETING_MATCH
import org.http4k.connect.openfeature.model.TargetingKey
import org.http4k.connect.successValue
import org.http4k.core.HttpHandler
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

interface OpenFeatureContract {
    val http: HttpHandler

    val targetingKey: TargetingKey get() = TargetingKey.of("user-123")

    val baseUri: Uri get() = Uri.of("http://localhost")

    fun seed(key: FlagKey, value: Any?)

    fun seedRule(key: FlagKey, value: Any?, predicate: (EvaluationContext) -> Boolean)

    @Test
    fun `evaluates a known boolean flag`() {
        val key = FlagKey.of("dark-mode")
        seed(key, true)
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(targetingKey))).successValue()

        assertThat(result.key, equalTo(key))
        assertThat(result.value, equalTo<Any?>(true))
        assertThat(result.reason, equalTo(STATIC))
    }

    @Test
    fun `evaluates a known string flag`() {
        val key = FlagKey.of("greeting")
        seed(key, "hello")
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(targetingKey))).successValue()

        assertThat(result.value, equalTo<Any?>("hello"))
    }

    @Test
    fun `unknown flag returns 404 remote failure`() {
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(FlagKey.of("missing"), EvaluationContext(targetingKey)))

        assertThat(result is Failure, equalTo(true))
        assertThat((result as Failure).reason.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `bulk evaluation returns all seeded flags`() {
        seed(FlagKey.of("a"), true)
        seed(FlagKey.of("b"), 42)
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateAllFlags(EvaluationContext(targetingKey))).successValue()

        val byKey = result.flags.associateBy { it.key }
        assertThat(byKey[FlagKey.of("a")]?.value, equalTo<Any?>(true))
        assertThat(byKey[FlagKey.of("b")]?.value, equalTo<Any?>(42.0))
    }

    @Test
    fun `matching rule on targeting key returns override with TARGETING_MATCH`() {
        val key = FlagKey.of("dark-mode")
        seed(key, false)
        seedRule(key, true) { ctx -> ctx.context["targetingKey"] == "alice" }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(TargetingKey.of("alice")))).successValue()

        assertThat(result.value, equalTo<Any?>(true))
        assertThat(result.reason, equalTo(TARGETING_MATCH))
    }

    @Test
    fun `non-matching context falls back to default value`() {
        val key = FlagKey.of("dark-mode")
        seed(key, false)
        seedRule(key, true) { ctx -> ctx.context["targetingKey"] == "alice" }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(TargetingKey.of("bob")))).successValue()

        assertThat(result.value, equalTo<Any?>(false))
        assertThat(result.reason, equalTo(STATIC))
    }

    @Test
    fun `rule matching arbitrary attribute beats default`() {
        val key = FlagKey.of("badge")
        seed(key, "small")
        seedRule(key, "big") { ctx -> ctx.context["plan"] == "premium" }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext("plan" to "premium"))).successValue()

        assertThat(result.value, equalTo<Any?>("big"))
        assertThat(result.reason, equalTo(TARGETING_MATCH))
    }

    @Test
    fun `rule without a default still 404s when no rule matches`() {
        val key = FlagKey.of("only-by-rule")
        seedRule(key, "matched") { ctx -> ctx.context["targetingKey"] == "alice" }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(TargetingKey.of("bob"))))

        assertThat(result is Failure, equalTo(true))
        assertThat((result as Failure).reason.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `first matching rule wins`() {
        val key = FlagKey.of("ordered")
        seed(key, "default")
        seedRule(key, "first") { true }
        seedRule(key, "second") { true }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateFlag(key, EvaluationContext(targetingKey))).successValue()

        assertThat(result.value, equalTo<Any?>("first"))
    }

    @Test
    fun `bulk evaluation applies rules using request context`() {
        val matched = FlagKey.of("matched")
        val unmatched = FlagKey.of("unmatched")
        seed(matched, "default")
        seed(unmatched, "default")
        seedRule(matched, "overridden") { ctx -> ctx.context["targetingKey"] == "alice" }
        seedRule(unmatched, "never") { ctx -> ctx.context["targetingKey"] == "carol" }
        val client = OpenFeature.Http(baseUri, http)

        val result = client(EvaluateAllFlags(EvaluationContext(TargetingKey.of("alice")))).successValue()

        val byKey = result.flags.associateBy { it.key }
        assertThat(byKey[matched]?.value, equalTo<Any?>("overridden"))
        assertThat(byKey[matched]?.reason, equalTo(TARGETING_MATCH))
        assertThat(byKey[unmatched]?.value, equalTo<Any?>("default"))
        assertThat(byKey[unmatched]?.reason, equalTo(STATIC))
    }
}
