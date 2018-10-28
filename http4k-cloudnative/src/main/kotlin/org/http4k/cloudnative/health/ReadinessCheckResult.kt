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
sealed class ReadinessCheckResult : Iterable<ReadinessCheckResult> {
    abstract val name: String
    abstract val pass: Boolean
    override fun iterator() = emptyList<ReadinessCheckResult>().iterator()
}

/**
 * The check completed successfully
 */
data class Completed(override val name: String) : ReadinessCheckResult() {
    override val pass = true
}

/**
 * The check failed
 */
data class Failed(override val name: String, val cause: Exception) : ReadinessCheckResult() {
    constructor(name: String, message: String): this(name, Exception(message))
    override val pass = false
}

/**
 * Result of multiple checks which calculates the overall result
 */
data class Composite(private val parts: Iterable<ReadinessCheckResult> = emptyList()) : ReadinessCheckResult() {
    override val name = "overall"
    override val pass by lazy { parts.fold(true) { acc, next -> acc && next.pass } }
    override fun iterator() = parts.iterator()
}

operator fun ReadinessCheckResult.plus(that: ReadinessCheckResult) = Composite(listOf(this, that))