package org.http4k.routing.experimental

import org.intellij.lang.annotations.Language

// Proof of concept to show how this could be done

@Language("HTML")
fun simpleDirectoryRenderer(path: String, filenames: List<String>) =
    """
    <html>
        <body>
            <h1>$path</h1>
            <ol>
                ${listOfFiles(filenames)}
            <ol>
        </body>
    </html>
    """.trimIndent()

private fun listOfFiles(filenames: List<String>): String =
    filenames.joinToString(separator = "\n") { name ->
        """<li><a href="$name">$name</a></li>"""
    }
