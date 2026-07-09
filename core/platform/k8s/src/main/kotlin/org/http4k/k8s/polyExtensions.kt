package org.http4k.k8s

import org.http4k.config.Environment
import org.http4k.config.Environment.Companion.ENV
import org.http4k.config.EnvironmentKey.k8s.HEALTH_PORT
import org.http4k.config.EnvironmentKey.k8s.SERVICE_PORT
import org.http4k.core.HttpHandler
import org.http4k.core.PolyHandler
import org.http4k.k8s.health.Health
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer

fun PolyHandler.asK8sServer(
    serverConfig: (port: Int) -> PolyServerConfig,
    port: Int = 8000,
    healthApp: HttpHandler = Health(),
    healthPort: Int = 8001
) = Http4kK8sServer(asServer(serverConfig(port)), healthApp.asServer(serverConfig(healthPort)))

fun PolyHandler.asK8sServer(
    serverConfig: (port: Int) -> PolyServerConfig,
    env: Environment = ENV,
    healthApp: HttpHandler = Health()
) = asK8sServer(serverConfig, SERVICE_PORT(env), healthApp, HEALTH_PORT(env))
