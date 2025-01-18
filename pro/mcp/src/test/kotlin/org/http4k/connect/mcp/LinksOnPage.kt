package org.http4k.connect.mcp

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.ResourceHandler
import org.http4k.mcp.ResourceResponse
import org.http4k.mcp.model.Resource
import org.jsoup.Jsoup

fun LinksOnPage(http: HttpHandler): ResourceHandler = {
    val htmlPage = http(Request(GET, it.uri))

    val links = Jsoup.parse(htmlPage.bodyString())
        .allElements.toList()
        .filter { it.tagName() == "a" }
        .filter { it.hasAttr("href") }
        .map {
            Resource.Content.Text(
                it.text(),
                Uri.of(it.attr("href"))
            )
        }
    ResourceResponse(links)
}
