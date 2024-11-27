package org.http4k.cloudnative.health

/**
 * A Readiness check is used to determine if the pod is ready to receive traffic. An example is to test
 * if the app can talk to it's database.
 */
interface ReadinessCheck : () -> ReadinessCheckResult {
    val name: String
}

/**
 * The result of a Readiness check. Checks can be combined together with `+()` to provide an overall result.
 */
sealed class ReadinessCheckResult(val pass: Boolean = true) : Iterable<ReadinessCheckResult> {
    abstract val name: String
    override fun iterator() = emptyList<ReadinessCheckResult>().iterator()
}

data class Completed(override val name: String) : ReadinessCheckResult(true)

data class Failed(override val name: String, val cause: Exception) : ReadinessCheckResult(false) {
    constructor(name: String, message: String) : this(name, Exception(message))
}

/**
 * Result of multiple checks, for which it reports an overall result (ie. any failure is fatal).
 */
data class Composite(internal val parts: Iterable<ReadinessCheckResult> = emptyList()) : ReadinessCheckResult(parts.fold(true) { acc, next -> acc && next.pass }) {
    override val name = "overall"
    override fun iterator() = parts.iterator()
}

operator fun ReadinessCheckResult.plus(that: ReadinessCheckResult): Composite = when (this) {
    is Composite -> Composite(parts = parts + listOf(that))
    else -> Composite(listOf(this, that))
}
