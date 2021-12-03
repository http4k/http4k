package org.http4k.serverless.openwhisk

import org.http4k.client.ApacheClient
import org.http4k.cloudnative.env.Authority
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.authority
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string
import org.http4k.serverless.openwhisk.OpenWhiskJson.auto

data class OpenWhiskConfig(val credentials: Credentials, val authority: Authority)

class OpenWhisk(
    config: OpenWhiskConfig,
    rawHttp: HttpHandler = ApacheClient()
) {
    private val insecureHttp = ClientFilters.SetBaseUriFrom(Uri.of("https://host/api/v1").authority(config.authority))
        .then(ClientFilters.HandleRemoteRequestFailed())
        .then(rawHttp)

    private val secureHttp = BasicAuth(config.credentials).then(insecureHttp)

    /**
     * Get all namespaces for authenticated user
     * Response:
     * 	application/json
     * 		200 Array of namespaces
     * 		401
     * 		500
     */
    fun getAllNamespaces(): List<String> {
        val getAllNamespacesJsonResponse200Lens = Body.auto<List<String>>().toLens()
        val httpReq = Request(Method.GET, "/namespaces")
        val response = secureHttp(httpReq)
        return getAllNamespacesJsonResponse200Lens(response)
    }

    /**
     * Invoke a web action and return the raw Response
     */
    fun invokeWebActionInPackage(namespace: String, packageName: String, actionName: String, request: Request) =
        insecureHttp(request.run { uri(uri.path("/web/$namespace/$packageName/$actionName" + uri.path)) })

    /**
     * Create or update an action
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Updated Action
     * 		400
     * 		401
     * 		403
     * 		409
     * 		413
     * 		500
     */
    fun updateActionInPackage(
        namespace: String,
        packageName: String,
        actionName: String,
        overwrite: String? = null,
        request: ActionPut
    ): Action {
        val actionPutLens = Body.auto<ActionPut>().toLens()
        val actionLens = Body.auto<Action>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val overwriteLens = Query.string().optional("overwrite")
        val httpReq = Request(Method.PUT, "/namespaces/{namespace}/actions/{packageName}/{actionName}")
            .with(actionPutLens of request)
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(overwriteLens of overwrite)
        val response = secureHttp(httpReq)
        return actionLens(response)
    }

    /**
     * Get all packages
     * Response:
     * 	application/json
     * 		200 Packages response
     * 		401
     * 		403
     * 		500
     */
    fun getAllPackages(
        namespace: String,
        public: Boolean? = null,
        limit: Int? = null,
        skip: Int? = null
    ): List<Package> {
        val packageLens = Body.auto<List<Package>>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val publicLens = Query.boolean().optional("public")
        val limitLens = Query.int().optional("limit")
        val skipLens = Query.int().optional("skip")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/packages")
            .with(namespaceLens of namespace)
            .with(publicLens of public)
            .with(limitLens of limit)
            .with(skipLens of skip)
        val response = secureHttp(httpReq)
        return packageLens(response)
    }

    /**
     * Get package information
     * Get package information.
     * Response:
     * 	application/json
     * 		200 Returned package
     * 		401
     * 		403
     * 		404
     * 		409
     * 		500
     */
    fun getPackageByName(namespace: String, packageName: String): Package {
        val packageLens = Body.auto<Package>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/packages/{packageName}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
        val response = secureHttp(httpReq)
        return packageLens(response)
    }

    /**
     * Create or update a package
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Updated Package
     * 		400
     * 		401
     * 		403
     * 		409
     * 		413
     * 		500
     */
    fun updatePackage(
        namespace: String,
        packageName: String,
        overwrite: String? = null,
        request: PackagePut
    ): Package {
        val packagePutLens = Body.auto<PackagePut>().toLens()
        val packageLens = Body.auto<Package>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val overwriteLens = Query.string().optional("overwrite")
        val httpReq = Request(Method.PUT, "/namespaces/{namespace}/packages/{packageName}")
            .with(packagePutLens of request)
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(overwriteLens of overwrite)
        val response = secureHttp(httpReq)
        return packageLens(response)
    }
}
