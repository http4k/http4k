package org.http4k.routing.experimental

import org.http4k.core.Uri
import org.intellij.lang.annotations.Language


typealias DirectoryRenderer = (uri: Uri, dir: ResourceSummary, resources: List<ResourceSummary>) -> String

@Language("HTML")
fun simpleDirectoryRenderer(uri: Uri, dir: ResourceSummary, resources: List<ResourceSummary>) = """
<html>
<body>
<h1>${dir.name}</h1>
<ol>
${listOfFiles(uri, resources)}
<ol>
</body>
</html>""".trimIndent()

private fun listOfFiles(base: Uri, resources: List<ResourceSummary>): String =
    resources.joinToString(separator = "\n") { resourceInfo ->
        """<li><a href="${base.path.pathJoin(resourceInfo.name)}">${resourceInfo.name}</a></li>"""
    }
