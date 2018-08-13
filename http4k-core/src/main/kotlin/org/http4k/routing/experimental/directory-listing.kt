package org.http4k.routing.experimental

import org.http4k.core.Request
import org.http4k.core.Uri
import org.intellij.lang.annotations.Language

// Proof of concept to show how this could be done

@Language("HTML")
fun simpleDirectoryRenderer(request: Request, filenames: List<String>) =
    """
    <html>
        <body>
            <h1>${request.uri}</h1>
            <ol>
                ${listOfFiles(request.uri, filenames)}
            <ol>
        </body>
    </html>
    """.trimIndent()

private fun listOfFiles(base: Uri, filenames: List<String>): String =
    filenames.joinToString(separator = "\n") { name ->
        """<li><a href="${hrefFor(base, name)}">$name</a></li>"""
    }

private fun hrefFor(base: Uri, filename: String) = Uri.of(base.toString().pathJoin(filename))
