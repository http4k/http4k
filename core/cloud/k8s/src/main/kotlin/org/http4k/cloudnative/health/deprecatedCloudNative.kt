package org.http4k.cloudnative.health

@Deprecated("Use org.http4k.k8s.health.ReadinessCheckResultRenderer instead", ReplaceWith("org.http4k.k8s.health.ReadinessCheckResultRenderer"))
typealias ReadinessCheckResultRenderer = org.http4k.k8s.health.ReadinessCheckResultRenderer

@Deprecated("Use org.http4k.k8s.health.DefaultReadinessCheckResultRenderer instead", ReplaceWith("org.http4k.k8s.health.DefaultReadinessCheckResultRenderer"))
typealias DefaultReadinessCheckResultRenderer = org.http4k.k8s.health.DefaultReadinessCheckResultRenderer

@Deprecated("Use org.http4k.k8s.health.JsonReadinessCheckResultRenderer instead", ReplaceWith("org.http4k.k8s.health.JsonReadinessCheckResultRenderer"))
typealias JsonReadinessCheckResultRenderer = org.http4k.k8s.health.JsonReadinessCheckResultRenderer

@Deprecated("Use org.http4k.k8s.health.Health instead", ReplaceWith("org.http4k.k8s.health.Health"))
typealias Health = org.http4k.k8s.health.Health

@Deprecated("Use org.http4k.k8s.health.Liveness instead", ReplaceWith("org.http4k.k8s.health.Liveness"))
typealias Liveness = org.http4k.k8s.health.Liveness

@Deprecated("Use org.http4k.k8s.health.Readiness instead", ReplaceWith("org.http4k.k8s.health.Readiness"))
typealias Readiness = org.http4k.k8s.health.Readiness

@Deprecated("Use org.http4k.k8s.health.ReadinessCheck instead", ReplaceWith("org.http4k.k8s.health.ReadinessCheck"))
typealias ReadinessCheck = org.http4k.k8s.health.ReadinessCheck

@Deprecated("Use org.http4k.k8s.health.check instead", ReplaceWith("org.http4k.k8s.health.check"))
typealias ReadinessCheckResult = org.http4k.k8s.health.ReadinessCheckResult

@Deprecated("Use org.http4k.k8s.health.check instead", ReplaceWith("org.http4k.k8s.health.check"))
typealias Completed = org.http4k.k8s.health.Completed

@Deprecated("Use org.http4k.k8s.health.check instead", ReplaceWith("org.http4k.k8s.health.check"))
typealias Failed = org.http4k.k8s.health.Failed

@Deprecated("Use org.http4k.k8s.health.check instead", ReplaceWith("org.http4k.k8s.health.check"))
typealias Composite = org.http4k.k8s.health.Composite

@Deprecated("Use org.http4k.k8s.health.plus instead", ReplaceWith("org.http4k.k8s.health.plus"))
operator fun ReadinessCheckResult.plus(that: ReadinessCheckResult): Composite = when (this) {
    is Composite -> Composite(parts = parts + listOf(that))
    else -> Composite(listOf(this, that))
}
