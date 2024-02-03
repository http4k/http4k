package org.http4k.contract.ui

import org.http4k.core.Filter

// See https://swagger.io/docs/open-source-tools/swagger-ui/usage/configuration/
class SwaggerUiConfig {

    var pageTitle: String = "Swagger UI"
    internal val properties = mutableMapOf<String, UiProperty<out Any>>()

    // core
    var url: String by MapDelegate(properties, "url", { "\"$it\"" }, default = "https://petstore.swagger.io/v2/swagger.json",)
    var domId: String by MapDelegate(properties,"dom_id", { "\"#$it\"" }, default = "swagger-ui",)
    var queryConfigEnabled: Boolean? by MapDelegate(properties,"queryConfigEnabled")

    // display
    var displayOperationId: Boolean? by MapDelegate(properties, "displayOperationId")

    var displayRequestDuration: Boolean? by MapDelegate(properties, "displayRequestDuration")
    var requestSnippetsEnabled: Boolean? by MapDelegate(properties, "requestSnippetsEnabled")
    var tryItOutEnabled: Boolean? by MapDelegate(properties, "tryItOutEnabled")
    var deepLinking: Boolean? by MapDelegate(properties, "deepLinking")

    // Network
    var oauth2RedirectUrl: String? by MapDelegate(properties, "oauth2RedirectUrl")
    var withCredentials: Boolean? by MapDelegate(properties, "withCredentials")

    // Authorization
    var persistAuthorization: Boolean? by MapDelegate(properties, "persistAuthorization")

    // plugins
    var layout: String by MapDelegate(properties, "layout", { "\"$it\"" }, default = "BaseLayout")
    var presets: List<String> by MapDelegate(
        properties = properties,
        name = "presets",
        format = { "[" + it.joinToString(",") + "]" },
        default = listOf("SwaggerUIBundle.presets.apis")
    )

    val additionalProperties = mutableMapOf<String, String>()
}

fun SwaggerUiConfig.toFilter() = Filter { next ->
    { req ->
        next(req).let { resp ->
            val properties = (properties.mapValues { it.value.formatted } + additionalProperties)
                .entries
                .joinToString(",\n        ") { (key, value) -> "$key: $value" }

            resp.body(
                resp.bodyString()
                    .replace("%%PAGE_TITLE%%", pageTitle)
                    .replace("%%DOM_ID%%", domId)
                    .replace("%%PROPERTIES%%", properties)
            )
        }
    }
}
