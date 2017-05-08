package org.http4k.http.contract

import org.http4k.http.core.Response
import org.http4k.http.lens.Failure

interface ModuleRenderer {
    fun notFound(): Response

    fun badRequest(failures: List<Failure>): Response

    fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>): Response
}

