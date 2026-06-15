/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.render

private const val PRISM_VERSION = "1.29.0"
private const val MERMAID_VERSION = "11"

private const val prismCss =
    """<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/$PRISM_VERSION/themes/prism.min.css">"""
private const val prismJs =
    """<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/$PRISM_VERSION/prism.min.js"></script>"""
private const val prismAutoloader =
    """<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/$PRISM_VERSION/plugins/autoloader/prism-autoloader.min.js"></script>"""
private const val mermaidJs =
    """<script type="module">import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@$MERMAID_VERSION/dist/mermaid.esm.min.mjs'; mermaid.initialize({startOnLoad: true});</script>"""

private val extensionToLanguage = mapOf(
    "kt" to "kotlin",
    "kts" to "kotlin",
    "java" to "java",
    "scala" to "scala",
    "groovy" to "groovy",
    "py" to "python",
    "rb" to "ruby",
    "rs" to "rust",
    "go" to "go",
    "js" to "javascript",
    "mjs" to "javascript",
    "ts" to "typescript",
    "tsx" to "tsx",
    "jsx" to "jsx",
    "c" to "c",
    "h" to "c",
    "cpp" to "cpp",
    "cc" to "cpp",
    "cs" to "csharp",
    "sh" to "bash",
    "bash" to "bash",
    "zsh" to "bash",
    "sql" to "sql",
    "md" to "markdown",
    "html" to "markup",
    "xml" to "markup",
    "svg" to "markup",
    "css" to "css",
    "scss" to "scss",
    "yaml" to "yaml",
    "yml" to "yaml",
    "json" to "json",
    "toml" to "toml",
    "ini" to "ini",
    "properties" to "properties"
)

internal fun languageFor(extension: String) = extensionToLanguage[extension.lowercase()] ?: "markup"

internal fun wrapAsHtmlDoc(content: String): String =
    if (content.trimStart().startsWith("<!DOCTYPE", ignoreCase = true)) content
    else """<!DOCTYPE html><html lang="en"><head><meta charset="UTF-8">$prismCss</head><body>$content$prismJs$prismAutoloader$mermaidJs</body></html>"""

internal fun escapeHtml(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
