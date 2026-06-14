/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.storyboard.StoryFrame.Kind.Manual
import org.junit.jupiter.api.Test

class RenderHtmlTest {

    private fun render(title: String, vararg frames: StoryFrame) = renderHtml(Story(title, frames.toList()))

    @Test
    fun `puts the test title in head and header`() {
        val html = render("Login flow works")

        assertThat(html, containsSubstring("<title>Storyboard: Login flow works</title>"))
        assertThat(html, containsSubstring(">Login flow works</span>"))
    }

    @Test
    fun `lists each frame title as a sidebar button`() {
        val html = render(
            "demo",
            StoryFrame("Home page", "", "<html/>".base64Encode(), Manual),
            StoryFrame("After login", "", "<html/>".base64Encode(), Manual)
        )

        assertThat(html, containsSubstring("data-index=\"0\""))
        assertThat(html, containsSubstring(">Home page</span>"))
        assertThat(html, containsSubstring("data-index=\"1\""))
        assertThat(html, containsSubstring(">After login</span>"))
    }

    @Test
    fun `renders a sandboxed thumbnail iframe for each frame`() {
        val html = render(
            "demo",
            StoryFrame("a", "", "<html/>".base64Encode(), Manual),
            StoryFrame("b", "", "<html/>".base64Encode(), Manual),
            StoryFrame("c", "", "<html/>".base64Encode(), Manual)
        )

        val thumbCount = Regex("class=\"thumb-iframe\"").findAll(html).count()
        assertThat(thumbCount, equalTo(3))
        assertThat(html, containsSubstring("<iframe sandbox=\"\" class=\"thumb-iframe\""))
    }

    @Test
    fun `init script assigns srcdoc to every thumbnail iframe`() {
        val html = render("demo", StoryFrame("only", "", "<html/>".base64Encode(), Manual))

        assertThat(html, containsSubstring("querySelector('iframe').srcdoc = atob(frames["))
    }

    @Test
    fun `init script wires arrow keys to navigate frames`() {
        val html = render("demo", StoryFrame("only", "", "<html/>".base64Encode(), Manual))

        assertThat(html, containsSubstring("addEventListener('keydown'"))
        assertThat(html, containsSubstring("'ArrowDown'"))
        assertThat(html, containsSubstring("'ArrowUp'"))
    }

    @Test
    fun `embeds story data as JSON in a script tag`() {
        val dom = "<html><body>x</body></html>".base64Encode()
        val html = render("demo", StoryFrame("only", "the notes", dom, Manual))

        assertThat(html, containsSubstring("<script type=\"application/json\" id=\"storyboard-data\">"))
        assertThat(html, containsSubstring("\"title\":\"only\""))
        assertThat(html, containsSubstring("\"notes\":\"the notes\""))
        assertThat(html, containsSubstring("\"dom\":\"$dom\""))
    }

    @Test
    fun `renders an iframe and notes container in the main view`() {
        val html = render("demo", StoryFrame("only", "", "<html/>".base64Encode(), Manual))

        assertThat(html, containsSubstring("<iframe"))
        assertThat(html, containsSubstring("id=\"storyboard-frame\""))
        assertThat(html, containsSubstring("id=\"storyboard-notes\""))
    }

    @Test
    fun `escapes script-close sequences in embedded JSON`() {
        val html = render("demo", StoryFrame("</script>", "", "x".base64Encode(), Manual))

        assertThat(html, containsSubstring("<\\/script>"))
    }

    @Test
    fun `escapes HTML in the test title`() {
        val html = render("a <b> & c")

        assertThat(html, containsSubstring(">a &lt;b&gt; &amp; c</span>"))
    }
}
