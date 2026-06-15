package org.http4k.storyboard.layout

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.http4k.storyboard.Chapter
import org.http4k.storyboard.Story
import org.http4k.storyboard.StoryFrame
import org.http4k.storyboard.frame.WebDriverCapture
import org.http4k.storyboard.layout.slideshow.Slideshow
import org.junit.jupiter.api.Test

class RenderHtmlTest {

    private fun render(title: String, vararg frames: StoryFrame) =
        renderHtml(Story(title, chapters = listOf(Chapter(title, frames.toList()))))

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
            WebDriverCapture("Home page", "", "<html/>".base64Encode(), StoryFrame.Level.Story),
            WebDriverCapture("After login", "", "<html/>".base64Encode(), StoryFrame.Level.Story)
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
            WebDriverCapture("a", "", "<html/>".base64Encode(), StoryFrame.Level.Story),
            WebDriverCapture("b", "", "<html/>".base64Encode(), StoryFrame.Level.Story),
            WebDriverCapture("c", "", "<html/>".base64Encode(), StoryFrame.Level.Story)
        )

        val thumbCount = Regex("class=\"thumb-iframe\"").findAll(html).count()
        assertThat(thumbCount, equalTo(3))
        assertThat(html, containsSubstring("<iframe sandbox=\"allow-scripts\" class=\"thumb-iframe\""))
    }

    @Test
    fun `init script assigns srcdoc to every thumbnail iframe`() {
        val html = render("demo", WebDriverCapture("only", "", "<html/>".base64Encode(), StoryFrame.Level.Story))

        assertThat(html, containsSubstring("querySelector('iframe').srcdoc = atob(frames["))
    }

    @Test
    fun `init script wires arrow keys to navigate frames`() {
        val html = render("demo", WebDriverCapture("only", "", "<html/>".base64Encode(), StoryFrame.Level.Story))

        assertThat(html, containsSubstring("addEventListener('keydown'"))
        assertThat(html, containsSubstring("'ArrowDown'"))
        assertThat(html, containsSubstring("'ArrowUp'"))
    }

    @Test
    fun `embeds derived frames as JSON in a script tag`() {
        val dom = "<html><body>x</body></html>".base64Encode()
        val html = render("demo", WebDriverCapture("only", "the notes", dom, StoryFrame.Level.Story))

        assertThat(html, containsSubstring("<script type=\"application/json\" id=\"storyboard-frames\">"))
        assertThat(html, containsSubstring("\"title\":\"only\""))
        assertThat(html, containsSubstring("\"notes\":\"the notes\""))
        assertThat(html, containsSubstring("\"dom\":\"$dom\""))
    }

    @Test
    fun `renders an iframe and notes container in the main view`() {
        val html = render("demo", WebDriverCapture("only", "", "<html/>".base64Encode(), StoryFrame.Level.Story))

        assertThat(html, containsSubstring("<iframe"))
        assertThat(html, containsSubstring("id=\"storyboard-frame\""))
        assertThat(html, containsSubstring("id=\"storyboard-notes\""))
    }

    @Test
    fun `escapes script-close sequences in embedded JSON`() {
        val html = render("demo", WebDriverCapture("</script>", "", "x".base64Encode(), StoryFrame.Level.Story))

        assertThat(html, containsSubstring("<\\/script>"))
    }

    @Test
    fun `escapes HTML in the test title`() {
        val html = render("a <b> & c")

        assertThat(html, containsSubstring(">a &lt;b&gt; &amp; c</span>"))
    }

    @Test
    fun `chapter dividers appear in the strip when frames belong to different chapters`() {
        val story = Story(
            title = "demo",
            chapters = listOf(
                Chapter(
                    title = "Root",
                    children = listOf(
                        Chapter(
                            "First",
                            listOf(WebDriverCapture("a", "", "<html/>".base64Encode(), StoryFrame.Level.Story))
                        ),
                        Chapter(
                            "Second",
                            listOf(WebDriverCapture("b", "", "<html/>".base64Encode(), StoryFrame.Level.Story))
                        )
                    )
                )
            )
        )

        val html = renderHtml(story)
        assertThat(html, containsSubstring("chapter-divider"))
        assertThat(html, containsSubstring(">First</span>"))
        assertThat(html, containsSubstring(">Second</span>"))
    }

    @Test
    fun `no chapter dividers when all frames live in the same chapter`() {
        val story = Story(
            title = "demo",
            chapters = listOf(
                Chapter("Root", listOf(WebDriverCapture("a", "", "<html/>".base64Encode(), StoryFrame.Level.Story)))
            )
        )

        val html = renderHtml(story)
        assertThat(html.contains("chapter-divider\""), equalTo(false))
    }

    private fun renderHtml(story: Story): String = Slideshow().render(story)

}
