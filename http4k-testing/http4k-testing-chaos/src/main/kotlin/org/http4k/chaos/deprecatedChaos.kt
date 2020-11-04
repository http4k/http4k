package org.http4k.chaos

import org.http4k.contract.security.NoSecurity
import org.http4k.contract.security.Security
import org.http4k.core.HttpHandler
import org.http4k.filter.CorsPolicy
import org.http4k.routing.RoutingHttpHandler

@Deprecated("Rename", ReplaceWith("withChaosEngine(ChaosEngine(stage), security, controlsPath, openApiPath, corsPolicy"))
fun RoutingHttpHandler.withChaosControls(stage: Stage = ChaosStages.Wait,
                                         security: Security = NoSecurity,
                                         controlsPath: String = "/chaos",
                                         openApiPath: String = "",
                                         corsPolicy: CorsPolicy = CorsPolicy.UnsafeGlobalPermissive
) = withChaosApi(ChaosEngine(stage), security, controlsPath, openApiPath, corsPolicy)

@Deprecated("Rename", ReplaceWith("withChaosEngine(ChaosEngine(stage), security, controlsPath, openApiPath, corsPolicy"))
fun HttpHandler.withChaosControls(stage: Stage = ChaosStages.Wait,
                                  security: Security = NoSecurity,
                                  controlsPath: String = "/chaos",
                                  openApiPath: String = "",
                                  corsPolicy: CorsPolicy = CorsPolicy.UnsafeGlobalPermissive) = withChaosApi(ChaosEngine(stage), security, controlsPath, openApiPath, corsPolicy)
