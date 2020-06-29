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
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.format.Jackson.auto
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.boolean
import org.http4k.lens.int
import org.http4k.lens.string

data class OpenWhiskConfig(val credentials: Credentials, val authority: Authority)

class OpenWhisk(
    config: OpenWhiskConfig,
    rawHttp: HttpHandler = ApacheClient()
) {
    private val http = ClientFilters.SetBaseUriFrom(Uri.of("https://host/api/v1").authority(config.authority))
        .then(ClientFilters.BasicAuth(config.credentials))
        .then(ClientFilters.HandleRemoteRequestFailed())
        .then(rawHttp)

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
        val response = http(httpReq)
        return getAllNamespacesJsonResponse200Lens(response)
    }

    /**
     * Get all actions
     * Response:
     * 	application/json
     * 		200 Actions response
     * 		401
     * 		500
     */
    fun getAllActions(
        namespace: String,
        limit: Int? = null,
        skip: Int? = null
    ): List<Action> {
        val actionLens = Body.auto<List<Action>>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val limitLens = Query.int().optional("limit")
        val skipLens = Query.int().optional("skip")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/actions")
            .with(namespaceLens of namespace)
            .with(limitLens of limit)
            .with(skipLens of skip)
        val response = http(httpReq)
        return actionLens(response)
    }

    /**
     * Get action information
     * Get action information.
     * Response:
     * 	application/json
     * 		200 Returned action
     * 		401
     * 		403
     * 		404
     * 		500
     */
    fun getActionByName(
        namespace: String,
        actionName: String,
        code: Boolean? = null
    ): Action {
        val actionLens = Body.auto<Action>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val actionNameLens = Path.string().of("actionName")
        val codeLens = Query.boolean().optional("code")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/actions/{actionName}")
            .with(namespaceLens of namespace)
            .with(actionNameLens of actionName)
            .with(codeLens of code)
        val response = http(httpReq)
        return actionLens(response)
    }

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
    fun updateAction(
        namespace: String,
        actionName: String,
        overwrite: String? = null,
        request: ActionPut
    ): Action {
        val actionPutLens = Body.auto<ActionPut>().toLens()
        val actionLens = Body.auto<Action>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val actionNameLens = Path.string().of("actionName")
        val overwriteLens = Query.string().optional("overwrite")
        val httpReq = Request(Method.PUT, "/namespaces/{namespace}/actions/{actionName}")
            .with(actionPutLens of request)
            .with(namespaceLens of namespace)
            .with(actionNameLens of actionName)
            .with(overwriteLens of overwrite)
        val response = http(httpReq)
        return actionLens(response)
    }

    /**
     * Invoke an action
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Successful activation
     * 		202
     * 		401
     * 		403
     * 		404
     * 		408
     * 		429
     * 		500
     * 		502 Activation produced an application error
     */
    fun invokeAction(
        namespace: String,
        actionName: String,
        request: InvokeActionJsonRequest,
        blocking: String? = null,
        result: String? = null,
        timeout: Int? = null
    ) {
        val invokeActionJsonRequestLens = Body.auto<InvokeActionJsonRequest>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val actionNameLens = Path.string().of("actionName")
        val blockingLens = Query.string().optional("blocking")
        val resultLens = Query.string().optional("result")
        val timeoutLens = Query.int().optional("timeout")
        val httpReq = Request(Method.POST, "/namespaces/{namespace}/actions/{actionName}")
            .with(invokeActionJsonRequestLens of request)
            .with(namespaceLens of namespace)
            .with(actionNameLens of actionName)
            .with(blockingLens of blocking)
            .with(resultLens of result)
            .with(timeoutLens of timeout)
        val response = http(httpReq)

        response
    }

    /**
     * Delete an action
     * Response:
     * 	application/json
     * 		200
     * 		400
     * 		401
     * 		403
     * 		404
     * 		409
     * 		500
     */
    fun deleteAction(namespace: String, actionName: String) {
        val namespaceLens = Path.string().of("namespace")
        val actionNameLens = Path.string().of("actionName")
        val httpReq = Request(Method.DELETE, "/namespaces/{namespace}/actions/{actionName}")
            .with(namespaceLens of namespace)
            .with(actionNameLens of actionName)
        val response = http(httpReq)

        response
    }

    /**
     * Get action information
     * Get action information.
     * Response:
     * 	application/json
     * 		200 Returned action
     * 		401
     * 		403
     * 		404
     * 		500
     */
    fun getActionInPackageByName(
        namespace: String,
        packageName: String,
        actionName: String,
        code: Boolean? = null
    ): Action {
        val actionLens = Body.auto<Action>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val codeLens = Query.boolean().optional("code")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/actions/{packageName}/{actionName}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(codeLens of code)
        val response = http(httpReq)
        return actionLens(response)
    }

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
        val response = http(httpReq)
        return actionLens(response)
    }

    /**
     * Invoke an action
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Successful activation
     * 		202
     * 		401
     * 		403
     * 		404
     * 		408
     * 		429
     * 		500
     * 		502 Activation produced an application error
     */
    fun invokeActionInPackage(
        namespace: String,
        packageName: String,
        actionName: String,
        request: InvokeActionInPackageJsonRequest,
        blocking: String? = null,
        result: String? = null,
        timeout: Int? = null
    ) {
        val invokeActionInPackageJsonRequestLens = Body.auto<InvokeActionInPackageJsonRequest>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val blockingLens = Query.string().optional("blocking")
        val resultLens = Query.string().optional("result")
        val timeoutLens = Query.int().optional("timeout")
        val httpReq = Request(Method.POST, "/namespaces/{namespace}/actions/{packageName}/{actionName}")
            .with(invokeActionInPackageJsonRequestLens of request)
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(blockingLens of blocking)
            .with(resultLens of result)
            .with(timeoutLens of timeout)
        val response = http(httpReq)

        response
    }

    /**
     * Delete an action
     * Response:
     * 	application/json
     * 		200
     * 		400
     * 		401
     * 		403
     * 		404
     * 		409
     * 		500
     */
    fun deleteActionInPackage(
        namespace: String,
        packageName: String,
        actionName: String
    ) {
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val httpReq = Request(Method.DELETE, "/namespaces/{namespace}/actions/{packageName}/{actionName}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
        val response = http(httpReq)

        response
    }

    /**
     * Response:
     * 	application/json
     * 		200 any response
     */
    fun get_web_namespace_packageName_actionNameextension(
        namespace: String,
        packageName: String,
        actionName: String,
        extension: String
    ): Get_web_namespace_packageName_actionNameextensionJsonResponse200 {
        val get_web_namespace_packageName_actionNameextensionJsonResponse200Lens =
            Body.auto<Get_web_namespace_packageName_actionNameextensionJsonResponse200>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val extensionLens = Path.string().of("extension")
        val httpReq = Request(Method.GET, "/web/{namespace}/{packageName}/{actionName}.{extension}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(extensionLens of extension)
        val response = http(httpReq)
        return get_web_namespace_packageName_actionNameextensionJsonResponse200Lens(response)
    }

    /**
     * Response:
     * 	application/json
     * 		200 any response
     */
    fun put_web_namespace_packageName_actionNameextension(
        namespace: String,
        packageName: String,
        actionName: String,
        extension: String
    ): Put_web_namespace_packageName_actionNameextensionJsonResponse200 {
        val put_web_namespace_packageName_actionNameextensionJsonResponse200Lens =
            Body.auto<Put_web_namespace_packageName_actionNameextensionJsonResponse200>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val extensionLens = Path.string().of("extension")
        val httpReq = Request(Method.PUT, "/web/{namespace}/{packageName}/{actionName}.{extension}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(extensionLens of extension)
        val response = http(httpReq)
        return put_web_namespace_packageName_actionNameextensionJsonResponse200Lens(response)
    }

    /**
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 any response
     */
    fun post_web_namespace_packageName_actionNameextension(
        namespace: String,
        packageName: String,
        actionName: String,
        extension: String,
        request: Post_web_namespace_packageName_actionNameextensionJsonRequest
    ): Post_web_namespace_packageName_actionNameextensionJsonResponse200 {
        val post_web_namespace_packageName_actionNameextensionJsonRequestLens =
            Body.auto<Post_web_namespace_packageName_actionNameextensionJsonRequest>().toLens()
        val post_web_namespace_packageName_actionNameextensionJsonResponse200Lens =
            Body.auto<Post_web_namespace_packageName_actionNameextensionJsonResponse200>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val extensionLens = Path.string().of("extension")
        val httpReq = Request(Method.POST, "/web/{namespace}/{packageName}/{actionName}.{extension}")
            .with(post_web_namespace_packageName_actionNameextensionJsonRequestLens of request)
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(extensionLens of extension)
        val response = http(httpReq)
        return post_web_namespace_packageName_actionNameextensionJsonResponse200Lens(response)
    }

    /**
     * Response:
     * 	application/json
     * 		200 any response
     */
    fun delete_web_namespace_packageName_actionNameextension(
        namespace: String,
        packageName: String,
        actionName: String,
        extension: String
    ): Delete_web_namespace_packageName_actionNameextensionJsonResponse200 {
        val delete_web_namespace_packageName_actionNameextensionJsonResponse200Lens =
            Body.auto<Delete_web_namespace_packageName_actionNameextensionJsonResponse200>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val actionNameLens = Path.string().of("actionName")
        val extensionLens = Path.string().of("extension")
        val httpReq = Request(Method.DELETE, "/web/{namespace}/{packageName}/{actionName}.{extension}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
            .with(actionNameLens of actionName)
            .with(extensionLens of extension)
        val response = http(httpReq)
        return delete_web_namespace_packageName_actionNameextensionJsonResponse200Lens(response)
    }

    /**
     * Get all rules
     * Response:
     * 	application/json
     * 		200 Rules response
     * 		401
     * 		500
     */
    fun getAllRules(
        namespace: String,
        limit: Int? = null,
        skip: Int? = null
    ): Rule {
        val ruleLens = Body.auto<Rule>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val limitLens = Query.int().optional("limit")
        val skipLens = Query.int().optional("skip")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/rules")
            .with(namespaceLens of namespace)
            .with(limitLens of limit)
            .with(skipLens of skip)
        val response = http(httpReq)
        return ruleLens(response)
    }

    /**
     * Get rule information
     * Response:
     * 	application/json
     * 		200 Returned rule
     * 		401
     * 		404
     * 		500
     */
    fun getRuleByName(namespace: String, ruleName: String): Rule {
        val ruleLens = Body.auto<Rule>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val ruleNameLens = Path.string().of("ruleName")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/rules/{ruleName}")
            .with(namespaceLens of namespace)
            .with(ruleNameLens of ruleName)
        val response = http(httpReq)
        return ruleLens(response)
    }

    /**
     * Create or update a rule
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Updated rule
     * 		400
     * 		401
     * 		404
     * 		409
     * 		413
     * 		500
     */
    fun updateRule(
        namespace: String,
        ruleName: String,
        overwrite: String? = null,
        request: RulePut
    ): Rule {
        val rulePutLens = Body.auto<RulePut>().toLens()
        val ruleLens = Body.auto<Rule>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val ruleNameLens = Path.string().of("ruleName")
        val overwriteLens = Query.string().optional("overwrite")
        val httpReq = Request(Method.PUT, "/namespaces/{namespace}/rules/{ruleName}")
            .with(rulePutLens of request)
            .with(namespaceLens of namespace)
            .with(ruleNameLens of ruleName)
            .with(overwriteLens of overwrite)
        val response = http(httpReq)
        return ruleLens(response)
    }

    /**
     * Enable or disable a rule
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200
     * 		202
     * 		400
     * 		401
     * 		404
     * 		500
     */
    fun setState(
        namespace: String,
        ruleName: String,
        request: SetStateJsonRequest
    ) {
        val setStateJsonRequestLens = Body.auto<SetStateJsonRequest>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val ruleNameLens = Path.string().of("ruleName")
        val httpReq = Request(Method.POST, "/namespaces/{namespace}/rules/{ruleName}")
            .with(setStateJsonRequestLens of request)
            .with(namespaceLens of namespace)
            .with(ruleNameLens of ruleName)
        val response = http(httpReq)

        response
    }

    /**
     * Delete a rule
     * Response:
     * 	application/json
     * 		200
     * 		401
     * 		404
     * 		500
     */
    fun deleteRule(namespace: String, ruleName: String) {
        val namespaceLens = Path.string().of("namespace")
        val ruleNameLens = Path.string().of("ruleName")
        val httpReq = Request(Method.DELETE, "/namespaces/{namespace}/rules/{ruleName}")
            .with(namespaceLens of namespace)
            .with(ruleNameLens of ruleName)
        val response = http(httpReq)

        response
    }

    /**
     * Get all triggers
     * Response:
     * 	application/json
     * 		200 Triggers response
     * 		401
     * 		500
     */
    fun getAllTriggers(
        namespace: String,
        limit: Int? = null,
        skip: Int? = null
    ): Trigger {
        val triggerLens = Body.auto<Trigger>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val limitLens = Query.int().optional("limit")
        val skipLens = Query.int().optional("skip")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/triggers")
            .with(namespaceLens of namespace)
            .with(limitLens of limit)
            .with(skipLens of skip)
        val response = http(httpReq)
        return triggerLens(response)
    }

    /**
     * Get trigger information
     * Response:
     * 	application/json
     * 		200 Returned trigger
     * 		401
     * 		404
     * 		500
     */
    fun getTriggerByName(namespace: String, triggerName: String): Trigger {
        val triggerLens = Body.auto<Trigger>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val triggerNameLens = Path.string().of("triggerName")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/triggers/{triggerName}")
            .with(namespaceLens of namespace)
            .with(triggerNameLens of triggerName)
        val response = http(httpReq)
        return triggerLens(response)
    }

    /**
     * Create or update a trigger
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		200 Updated trigger
     * 		400
     * 		401
     * 		409
     * 		413
     * 		500
     */
    fun updateTrigger(
        namespace: String,
        triggerName: String,
        overwrite: String? = null,
        request: TriggerPut
    ): Trigger {
        val triggerPutLens = Body.auto<TriggerPut>().toLens()
        val triggerLens = Body.auto<Trigger>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val triggerNameLens = Path.string().of("triggerName")
        val overwriteLens = Query.string().optional("overwrite")
        val httpReq = Request(Method.PUT, "/namespaces/{namespace}/triggers/{triggerName}")
            .with(triggerPutLens of request)
            .with(namespaceLens of namespace)
            .with(triggerNameLens of triggerName)
            .with(overwriteLens of overwrite)
        val response = http(httpReq)
        return triggerLens(response)
    }

    /**
     * Fire a trigger
     * Request:
     * 	application/json
     * Response:
     * 	application/json
     * 		202 Activation id
     * 		204
     * 		401
     * 		404
     * 		408
     * 		429
     * 		500
     */
    fun fireTrigger(
        namespace: String,
        triggerName: String,
        request: FireTriggerJsonRequest
    ): ActivationId {
        val fireTriggerJsonRequestLens = Body.auto<FireTriggerJsonRequest>().toLens()
        val activationIdLens = Body.auto<ActivationId>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val triggerNameLens = Path.string().of("triggerName")
        val httpReq = Request(Method.POST, "/namespaces/{namespace}/triggers/{triggerName}")
            .with(fireTriggerJsonRequestLens of request)
            .with(namespaceLens of namespace)
            .with(triggerNameLens of triggerName)
        val response = http(httpReq)
        return activationIdLens(response)
    }

    /**
     * Delete a trigger
     * Response:
     * 	application/json
     * 		200
     * 		401
     * 		404
     * 		500
     */
    fun deleteTrigger(namespace: String, triggerName: String) {
        val namespaceLens = Path.string().of("namespace")
        val triggerNameLens = Path.string().of("triggerName")
        val httpReq = Request(Method.DELETE, "/namespaces/{namespace}/triggers/{triggerName}")
            .with(namespaceLens of namespace)
            .with(triggerNameLens of triggerName)
        val response = http(httpReq)

        response
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
        val response = http(httpReq)
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
        val response = http(httpReq)
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
        val response = http(httpReq)
        return packageLens(response)
    }

    /**
     * Delete a package
     * Response:
     * 	application/json
     * 		200
     * 		401
     * 		404
     * 		409
     * 		500
     */
    fun deletePackage(namespace: String, packageName: String) {
        val namespaceLens = Path.string().of("namespace")
        val packageNameLens = Path.string().of("packageName")
        val httpReq = Request(Method.DELETE, "/namespaces/{namespace}/packages/{packageName}")
            .with(namespaceLens of namespace)
            .with(packageNameLens of packageName)
        val response = http(httpReq)

        response
    }

    /**
     * Get activation summary
     * Get activation summary.
     * Response:
     * 	application/json
     * 		200 Activations response
     * 		401
     * 		500
     */
    fun getActivations(
        namespace: String,
        name: String? = null,
        limit: Int? = null,
        skip: Int? = null,
        since: Int? = null,
        upto: Int? = null,
        docs: Boolean? = null
    ): ActivationBrief {
        val activationBriefLens = Body.auto<ActivationBrief>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val nameLens = Query.string().optional("name")
        val limitLens = Query.int().optional("limit")
        val skipLens = Query.int().optional("skip")
        val sinceLens = Query.int().optional("since")
        val uptoLens = Query.int().optional("upto")
        val docsLens = Query.boolean().optional("docs")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/activations")
            .with(namespaceLens of namespace)
            .with(nameLens of name)
            .with(limitLens of limit)
            .with(skipLens of skip)
            .with(sinceLens of since)
            .with(uptoLens of upto)
            .with(docsLens of docs)
        val response = http(httpReq)
        return activationBriefLens(response)
    }

    /**
     * Get activation information
     * Get activation information.
     * Response:
     * 	application/json
     * 		200 Return output
     * 		401
     * 		404
     * 		500
     */
    fun getActivationById(namespace: String, activationid: String): Activation {
        val activationLens = Body.auto<Activation>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val activationidLens = Path.string().of("activationid")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/activations/{activationid}")
            .with(namespaceLens of namespace)
            .with(activationidLens of activationid)
        val response = http(httpReq)
        return activationLens(response)
    }

    /**
     * Get the logs for an activation
     * Get activation logs information.
     * Response:
     * 	application/json
     * 		200 Return output
     * 		401
     * 		404
     * 		500
     */
    fun getActivationLogs(namespace: String, activationid: String): ActivationLogs {
        val activationLogsLens = Body.auto<ActivationLogs>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val activationidLens = Path.string().of("activationid")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/activations/{activationid}/logs")
            .with(namespaceLens of namespace)
            .with(activationidLens of activationid)
        val response = http(httpReq)
        return activationLogsLens(response)
    }

    /**
     * Get the result of an activation
     * Get activation result.
     * Response:
     * 	application/json
     * 		200 Return output
     * 		401
     * 		404
     * 		500
     */
    fun getActivationResult(namespace: String, activationid: String): ActivationResult {
        val activationResultLens = Body.auto<ActivationResult>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val activationidLens = Path.string().of("activationid")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/activations/{activationid}/result")
            .with(namespaceLens of namespace)
            .with(activationidLens of activationid)
        val response = http(httpReq)
        return activationResultLens(response)
    }

    /**
     * Get the limits for a namespace
     * Get limits.
     * Response:
     * 	application/json
     * 		200 Return output
     * 		401
     * 		500
     */
    fun getLimits(namespace: String): UserLimits {
        val userLimitsLens = Body.auto<UserLimits>().toLens()
        val namespaceLens = Path.string().of("namespace")
        val httpReq = Request(Method.GET, "/namespaces/{namespace}/limits")
            .with(namespaceLens of namespace)
        val response = http(httpReq)
        return userLimitsLens(response)
    }
}
