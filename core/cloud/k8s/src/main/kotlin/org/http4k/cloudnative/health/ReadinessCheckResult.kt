package org.http4k.cloudnative.health

typealias ReadinessCheck = org.http4k.k8s.health.ReadinessCheck
typealias ReadinessCheckResult = org.http4k.k8s.health.ReadinessCheckResult
typealias Completed = org.http4k.k8s.health.Completed
typealias Failed = org.http4k.k8s.health.Failed
typealias Composite = org.http4k.k8s.health.Composite

@Deprecated("Use org.http4k.k8s.health.plus instead", ReplaceWith("org.http4k.k8s.health.plus"))
operator fun ReadinessCheckResult.plus(that: ReadinessCheckResult): Composite = when (this) {
    is Composite -> Composite(parts = parts + listOf(that))
    else -> Composite(listOf(this, that))
}
