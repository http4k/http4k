package org.http4k.storyboard.theme

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.storyboard.Story
import org.http4k.storyboard.Theme
import org.http4k.storyboard.layout.slideshow.Slideshow
import org.junit.jupiter.api.Test

class ThemeTest {

    @Test
    fun `default theme matches the historical hardcoded values`() {
        val t = Theme.Http4k
        assertThat(t.brandName, equalTo("Storyboard"))
        assertThat(t.brandHref, equalTo("https://http4k.org"))
        assertThat(t.logoUrl, equalTo("https://http4k.org/images/logo.png"))
        assertThat(t.faviconLarge, equalTo("https://http4k.org/favicon-32.png"))
        assertThat(t.faviconSmall, equalTo("https://http4k.org/favicon-16.png"))
        assertThat(
            t.headerBackground,
            equalTo("url(\"https://http4k.org/images/pipes-hero.svg\"), linear-gradient(135deg, #59AFF5 0%, #2096F3 50%, #61C0FF 100%)")
        )
        assertThat(t.accentColor, equalTo("#fd7e14"))
        assertThat(t.linkColor, equalTo("#0d6efd"))
        assertThat(t.textColor, equalTo("#212529"))
        assertThat(t.textMutedColor, equalTo("#6c757d"))
        assertThat(t.borderColor, equalTo("#dee2e6"))
        assertThat(t.bgLight, equalTo("#f8f9fa"))
        assertThat(t.bgHover, equalTo("#e9ecef"))
        assertThat(t.extraHeadHtml, equalTo(""))
    }

    @Test
    fun `custom theme values reach the rendered HTML`() {
        val story = Story(title = "demo")
        val html = Slideshow(acmeTheme).render(story)

        assertThat(html, containsSubstring(">Acme Docs</span>"))
        assertThat(html, containsSubstring("href=\"https://acme.example.com\""))
        assertThat(html, containsSubstring("src=\"${acmeTheme.logoUrl}\""))
        assertThat(html, containsSubstring("--color-accent: #00b894"))
        assertThat(html, containsSubstring("--color-link: #0984e3"))
        assertThat(html, containsSubstring(acmeTheme.extraHeadHtml))
    }
}
