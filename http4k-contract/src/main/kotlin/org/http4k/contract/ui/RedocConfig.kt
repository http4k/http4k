package org.http4k.contract.ui

import org.http4k.core.Filter

data class RedocConfig(
    var pageTitle: String = "Redoc",
    var url: String = "https://petstore.swagger.io/v2/swagger.json",
    val options: MutableMap<String, String> = mutableMapOf()
)

fun RedocConfig.toFilter(bundleUrl: String) = Filter { next ->
    { req ->
        next(req).let { resp ->
            resp.body(
                resp.bodyString()
                    .replace("%%DESCRIPTION_ROUTE%%", url)
                    .replace("%%PAGE_TITLE%%", pageTitle)
                    .replace("%%BUNDLE_URL%%", bundleUrl)
                    .replace("%%OPTIONS%%", options.map {(key, value) -> "$key=\"$value\"" }.joinToString(" "))
            )
        }
    }
}
