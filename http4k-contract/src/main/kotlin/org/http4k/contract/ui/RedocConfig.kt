package org.http4k.contract.ui

import org.http4k.core.Filter

data class RedocConfig(
    var pageTitle: String = "Redoc",
    var descriptionUrl: String = "https://petstore.swagger.io/v2/swagger.json",
)

fun RedocConfig.toFilter(bundleUrl: String) = Filter { next ->
    { req ->
        next(req).let { resp ->
            resp.body(
                resp.bodyString()
                    .replace("%%DESCRIPTION_ROUTE%%", descriptionUrl)
                    .replace("%%PAGE_TITLE%%", pageTitle)
                    .replace("%%BUNDLE_URL%%", bundleUrl)
//                    .replace("%%DISPLAY_OPERATION_ID%%", displayOperationId.toString())
//                    .replace("%%DISPLAY_REQUEST_DURATION%%", displayRequestDuration.toString())
//                    .replace("%%REQUEST_SNIPPETS_ENABLED%%", requestSnippetsEnabled.toString())
//                    .replace("%%PERSIST_AUTHORIZATION%%", persistAuthorization.toString())
//                    .replace("%%QUERY_CONFIG_ENABLED%%", queryConfigEnabled.toString())
//                    .replace("%%TRY_IT_OUT_ENABLED%%", tryItOutEnabled.toString())
//                    .replace("%%DEEP_LINKING%%", deepLinking.toString())
//                    .replace("%%OAUTH2_REDIRECT_URL%%", oauth2RedirectUrl.toString())
//                    .replace("%%WITH_CREDENTIALS%%", withCredentials.toString())
//                    .replace("%%LAYOUT%%", layout)
//                    .replace("%%PRESETS%%", presets.joinToString(","))
//                    .replace("%%DOM_ID%%", domId)
            )
        }
    }
}
