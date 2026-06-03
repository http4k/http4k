/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel.breakdown

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.Test

class MermaidDiagramViewTest {

    private val render = Templates()

    @Test
    fun `attacker-injected HTML in mermaid source is escaped, not emitted as raw markup`() {
        val injected = """</pre><script>alert('xss')</script><pre>"""

        val output = render(MermaidDiagramView(injected))

        assertThat(output, !containsSubstring("<script>alert('xss')</script>"))
        assertThat(output, !containsSubstring("</pre><script>"))
    }

    @Test
    fun `legitimate mermaid arrows survive escaping (browser decodes entities in text nodes)`() {
        val source = """sequenceDiagram
    A->>B: hi
    B-->>A: ok"""

        val output = render(MermaidDiagramView(source))

        assertThat(
            output.lineSequence().any { it.contains("A-&gt;&gt;B: hi") || it.contains("A->>B: hi") },
            equalTo(true)
        )
    }
}
