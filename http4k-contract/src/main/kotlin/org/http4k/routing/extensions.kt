package org.http4k.routing

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRouter
import org.http4k.contract.NoRenderer
import org.http4k.contract.NoSecurity
import org.http4k.core.ContentType
import org.http4k.core.Filter
import org.http4k.routing.StaticRouter.Companion.Handler

fun contractRoutes(contractRoot: String, renderer: ContractRenderer = NoRenderer) =
    ContractRouter(contractRoot, renderer, Filter { it }, NoSecurity, "", emptyList())

fun staticRoutes(root: String, resourceLoader: ResourceLoader = ResourceLoader.Classpath(), vararg extraPairs: Pair<String, ContentType>): RoutingHttpHandler =
    StaticRouter(Handler(root, resourceLoader, extraPairs.asList().toMap()))
