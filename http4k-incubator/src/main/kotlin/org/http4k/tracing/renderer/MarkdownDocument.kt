package org.http4k.tracing.renderer

import org.http4k.tracing.TraceRender
import org.http4k.tracing.TraceRenderer

fun MarkdownDocument(vararg renderers: TraceRenderer) = TraceRenderer { scenarioName, steps ->
    val markdownDoc = renderers
        .map { it.render(scenarioName, steps) }
        .fold("#$scenarioName") { acc, next ->
            acc + when (next.format) {
                "MD" ->
                    """
${next.content}
"""

                "MMD" ->
                    """
```mermaid
${next.content}
```
"""

                else ->
                    """
```${next.format}
${next.content}
```
"""
            }
        }

    TraceRender(scenarioName, "MD", markdownDoc)
}
