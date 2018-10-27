package org.http4k.k8s

/**
 * A Readiness check is used by K8S to determine if the pod is ready to receive traffic. An example is to test
 * if the app can talk to it's database.
 */
typealias ReadinessCheck = () -> ReadinessCheckResult

/**
 * The result of a Readiness check. Checks can be combined together with `+()` to provide an overall result.
 */
interface ReadinessCheckResult : Iterable<ReadinessCheckResult> {
    val name: String
    val pass: Boolean
    operator fun plus(that: ReadinessCheckResult): ReadinessCheckResult = Composite(listOf(this, that))
    override fun iterator() = emptyList<ReadinessCheckResult>().iterator()

    companion object {
        /**
         * a Default implementation of a result.
         */
        operator fun invoke(pass: Boolean, name: String = "success"): ReadinessCheckResult = Simple(name, pass)

        internal operator fun invoke(parts: Iterable<ReadinessCheckResult> = emptyList()): ReadinessCheckResult = Composite(parts)

        private data class Simple(override val name: String, override val pass: Boolean) : ReadinessCheckResult {
            override fun toString(): String = "$name=$pass"
        }

        private data class Composite(private val parts: Iterable<ReadinessCheckResult> = emptyList()) : ReadinessCheckResult {
            override val name = "success"
            override val pass by lazy { parts.fold(true) { acc, next -> acc && next.pass } }
            override fun iterator() = parts.iterator()
        }
    }
}
