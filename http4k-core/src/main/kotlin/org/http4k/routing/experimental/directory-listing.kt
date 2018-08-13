package org.http4k.routing.experimental

import org.http4k.core.Uri
import org.intellij.lang.annotations.Language

// Proof of concept to show how this could be done

@Language("HTML")
fun simpleDirectoryRenderer(base: Uri, filenames: List<String>) =
    """
    <html>
        <body>
            <h1>$base</h1>
            <ol>
                ${listOfFiles(base, filenames)}
            <ol>
        </body>
    </html>
    """.trimIndent()

private fun listOfFiles(base: Uri, filenames: List<String>): String =
    filenames.joinToString(separator = "\n") { name ->
        """<li><a href="${hrefFor(base, name)}">$name</a></li>"""
    }

private fun hrefFor(base: Uri, filename: String) = Uri.of(base.toString().pathJoin(filename))
