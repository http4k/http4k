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
