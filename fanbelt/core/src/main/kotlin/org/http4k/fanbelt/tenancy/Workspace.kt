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

/**
 * Routes each request to its tenant's Scope, by path variable or the app's own tenant and root functions. A tenant that
 * exists rejects is a 404.
 */
class Workspace(
    internal val infra: AppInfra,
    private val template: String,
    private val exists: (TenantId) -> Boolean,
    private val tenant: (Request) -> TenantId,
    private val root: (TenantId) -> Uri,
) {
    /**
     * Takes the tenant from the template's path variable, such as `/{tenant}`, and roots each tenant's pages there.
     */
    constructor(infra: AppInfra, template: String, exists: (TenantId) -> Boolean) : this(
        infra,
        template,
        exists,
        Path.value(TenantId).of(variableIn(template)),
        { tenant -> Uri.of(variable.replace(template, tenant.value)).appendToPath("/") },
    )

    /**
     * Mounts the tenant's routes under the template, answering 404 for a tenant that doesn't exist.
     */
    fun routes(tenantRoutes: PolyHandler) = poly(template bind knownTenant.then(tenantRoutes))

    /**
     * The Scope of the request's tenant.
     */
    fun scope(request: Request) = infra.scope(tenant(request))

    /**
     * The root of the request's tenant, for links and redirects.
     */
    fun uri(request: Request) = uri(tenant(request))

    /**
     * The root of a tenant, for links and redirects.
     */
    fun uri(tenant: TenantId): Uri = root(tenant)

    private fun known(request: Request) = lensOrNull { tenant(request) }?.let(exists) == true

    private val knownTenant = Guard({ Response(NOT_FOUND) }) { it.takeIf(::known) }
}

private val variable = Regex("\\{([^}:]+)[^}]*}")

private fun variableIn(template: String) =
    variable.find(template)?.groupValues?.get(1) ?: error("a workspace template needs a {variable} for the tenant: $template")
