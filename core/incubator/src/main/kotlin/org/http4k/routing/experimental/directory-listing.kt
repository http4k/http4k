package org.http4k.routing.experimental

import org.http4k.core.Uri
import org.intellij.lang.annotations.Language
import java.net.URLEncoder

typealias DirectoryRenderer = (uri: Uri, dir: ResourceSummary, resources: Iterable<ResourceSummary>) -> String

@Language("HTML")
fun simpleDirectoryRenderer(uri: Uri, dir: ResourceSummary, resources: Iterable<ResourceSummary>) = """
<html>
<body>
<h1>${dir.name.htmlEscape()}</h1>
<ol>
${listOfFiles(uri, resources)}
</ol>
</body>
</html>""".trimIndent()

private fun listOfFiles(base: Uri, resources: Iterable<ResourceSummary>): String =
    resources.joinToString(separator = "\n") { resourceInfo ->
        val href = base.path.pathJoin(resourceInfo.name.urlPathEncode())
        """<li><a href="${href.htmlEscape()}">${resourceInfo.name.htmlEscape()}</a></li>"""
    }

private fun String.htmlEscape() = buildString(length) {
    for (c in this@htmlEscape) {
        when (c) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(c)
        }
    }
}

private fun String.urlPathEncode() = URLEncoder.encode(this, Charsets.UTF_8).replace("+", "%20")
