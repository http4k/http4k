/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.core.appendToPath
import org.http4k.core.then
import org.http4k.fanbelt.http.Guard
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.input.lensOrNull
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.poly

class Workspace(
    internal val infra: AppInfra,
    private val template: String,
    private val exists: (TenantId) -> Boolean,
    private val tenant: (Request) -> TenantId,
    private val root: (TenantId) -> Uri,
) {
    constructor(infra: AppInfra, template: String, exists: (TenantId) -> Boolean) : this(
        infra,
        template,
        exists,
        Path.value(TenantId).of(variableIn(template)),
        { tenant -> Uri.of(variable.replace(template, tenant.value)).appendToPath("/") },
    )

    fun routes(tenantRoutes: PolyHandler) = poly(template bind knownTenant.then(tenantRoutes))

    fun scope(request: Request) = infra.scope(tenant(request))

    fun uri(request: Request) = uri(tenant(request))

    fun uri(tenant: TenantId): Uri = root(tenant)

    private fun known(request: Request) = lensOrNull { tenant(request) }?.let(exists) == true

    private val knownTenant = Guard({ Response(NOT_FOUND) }) { request -> request.takeIf(::known) }
}

private val variable = Regex("\\{([^}:]+)[^}]*}")

private fun variableIn(template: String) =
    variable.find(template)?.groupValues?.get(1) ?: error("a workspace template needs a {variable} for the tenant: $template")
