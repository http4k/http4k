/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.frame

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.StoryFrame.Level.Context
import org.http4k.storyboard.StoryFrame.Level.Story
import org.http4k.storyboard.html
import org.http4k.storyboard.recordFrames
import org.junit.jupiter.api.Test
import java.util.Base64

class HtmlFrameTest {

    private fun decoded(frame: StoryFrame): String = String(Base64.getDecoder().decode(frame.dom))

    @Test
    fun `html records a frame at Context level by default`() {
        val frame = recordFrames { html("Intro", "<p>hello</p>") }.single()

        assertThat(frame.title, equalTo("Intro"))
        assertThat(frame.notes, equalTo(""))
        assertThat(frame.level, equalTo(Context))
        assertThat(frame is Html, equalTo(true))
    }

    @Test
    fun `html with a fragment wraps in a full HTML document with Prism and Mermaid CDN`() {
        val html = decoded(recordFrames {
            html("Diagram", """<pre class="mermaid">sequenceDiagram</pre>""")
        }.single())

        assertThat(html, containsSubstring("<!DOCTYPE html>"))
        assertThat(html, containsSubstring("""<pre class="mermaid">sequenceDiagram</pre>"""))
        assertThat(html, containsSubstring("prism.min.css"))
        assertThat(html, containsSubstring("mermaid"))
    }

    @Test
    fun `html with a full document passes through unchanged`() {
        val full = """<!DOCTYPE html><html><head><title>own</title></head><body>own</body></html>"""

        assertThat(decoded(recordFrames { html("Custom", full) }.single()), equalTo(full))
    }

    @Test
    fun `html level can be overridden`() {
        val frame = recordFrames { html("Hero", "<h1>welcome</h1>", level = Story) }.single()

        assertThat(frame.level, equalTo(Story))
    }
}
