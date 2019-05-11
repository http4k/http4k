package org.http4k.contract

import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.v2.OpenApi
import org.http4k.contract.simple.SimpleJson

@Deprecated("use repackaged version", ReplaceWith("org.http4k.contract.simple.SimpleJson"))
typealias SimpleJson<NODE> = SimpleJson<NODE>

@Deprecated("use repackaged version", ReplaceWith("org.http4k.contract.openapi.ApiInfo"))
typealias ApiInfo = ApiInfo

@Deprecated("use repackaged version", ReplaceWith("org.http4k.contract.openapi.v2.OpenApi"))
typealias OpenApi<NODE> = OpenApi<NODE>

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList() }", ReplaceWith("BROKEN! use example"))
fun contract(vararg serverRoutes: ContractRoute) = contract {
    routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer, descriptionPath: String, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.descriptionPath = descriptionPath
    this.routes += serverRoutes.toList()
}

@Deprecated("Replaced with DSL version using contract { routes += serverRoutes.toList(); this.renderer = renderer; this.descriptionPath = descriptionPath; this.security = security }",
    ReplaceWith("BROKEN! use example"))
fun contract(renderer: ContractRenderer = NoRenderer, descriptionPath: String = "", security: Security = NoSecurity, vararg serverRoutes: ContractRoute) = contract {
    this.renderer = renderer
    this.security = security
    this.descriptionPath = descriptionPath
    this.routes += serverRoutes.toList()
}

@Deprecated("Renamed", ReplaceWith("ApiKeySecurity<T>"))
typealias ApiKey<T> = ApiKeySecurity<T>