package org.http4k.k8s

typealias ReadinessCheck = () -> ReadinessCheckResult

interface ReadinessCheckResult {
    val pass: Boolean

    companion object {
        private data class Simple(override val pass: Boolean) : ReadinessCheckResult {
            override fun toString() = "success=$pass"
        }

        operator fun invoke(pass: Boolean): ReadinessCheckResult = Simple(pass)
        operator fun invoke(parts: Iterable<ReadinessCheckResult> = emptyList()): Composite = Composite(parts)

        data class Composite(private val parts: Iterable<ReadinessCheckResult> = emptyList()) : ReadinessCheckResult {
            override val pass by lazy { parts.fold(true) { acc, next -> acc && next.pass } }
            operator fun plus(that: ReadinessCheckResult): Composite = copy(parts = parts + that)

            override fun toString(): String = Simple(pass).toString()
        }
    }
}
