package org.http4k.contract

import org.http4k.core.Response
import org.http4k.lens.Failure

interface ModuleRenderer {
    fun notFound(): Response

    fun badRequest(failures: List<Failure>): Response

    fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>): Response
}

