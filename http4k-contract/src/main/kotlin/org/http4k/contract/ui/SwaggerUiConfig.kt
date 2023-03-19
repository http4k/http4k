package org.http4k.contract.ui

import org.http4k.core.Filter

// See https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/
data class SwaggerUiConfig(
    var pageTitle: String? = null,

    // core
    var url: String = "https://petstore.swagger.io/v2/swagger.json",
    var domId: String = "swagger-ui",
    var queryConfigEnabled: Boolean? = null,

    // display
    var displayOperationId: Boolean? = null,
    var displayRequestDuration: Boolean? = null,
    var requestSnippetsEnabled: Boolean? = null,
    var tryItOutEnabled: Boolean? = null,
    var deepLinking: Boolean? = null,

    // Network
    var oauth2RedirectUrl: String? = null,
    var withCredentials: Boolean? = null,

    // Authorization
    var persistAuthorization: Boolean? = null,

    // plugins
    var layout: String = "BaseLayout",
    var presets: List<String> = listOf("SwaggerUIBundle.presets.apis")
)

fun SwaggerUiConfig.toFilter() = Filter { next ->
    { req ->
        next(req).let { resp ->
            resp.body(
                resp.bodyString()
                    .replace("%%DESCRIPTION_ROUTE%%", url)
                    .replace("%%PAGE_TITLE%%", pageTitle.toString())
                    .replace("%%DISPLAY_OPERATION_ID%%", displayOperationId.toString())
                    .replace("%%DISPLAY_REQUEST_DURATION%%", displayRequestDuration.toString())
                    .replace("%%REQUEST_SNIPPETS_ENABLED%%", requestSnippetsEnabled.toString())
                    .replace("%%PERSIST_AUTHORIZATION%%", persistAuthorization.toString())
                    .replace("%%QUERY_CONFIG_ENABLED%%", queryConfigEnabled.toString())
                    .replace("%%TRY_IT_OUT_ENABLED%%", tryItOutEnabled.toString())
                    .replace("%%DEEP_LINKING%%", deepLinking.toString())
                    .replace("%%OAUTH2_REDIRECT_URL%%", oauth2RedirectUrl.toString())
                    .replace("%%WITH_CREDENTIALS%%", withCredentials.toString())
                    .replace("%%LAYOUT%%", layout)
                    .replace("%%PRESETS%%", presets.joinToString(","))
                    .replace("%%DOM_ID%%", domId)
            )
        }
    }
}
