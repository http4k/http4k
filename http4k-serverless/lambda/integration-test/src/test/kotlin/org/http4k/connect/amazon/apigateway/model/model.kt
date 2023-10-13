package org.http4k.connect.amazon.apigateway.model

import org.http4k.core.Uri

data class ApiName(val value: String)

data class ApiId(val value: String)

data class StageName(val value: String)

data class Stage(val stageName: StageName, val autoDeploy: Boolean) {
    companion object {
        val restDefault = Stage(StageName("default"), true)
        val default = Stage(StageName("\$default"), true)
    }
}

data class ApiDetails(val name: ApiName, val apiId: ApiId, val apiEndpoint: Uri)

data class DeploymentId(val id: String)

data class DeploymentName(val value: String)
