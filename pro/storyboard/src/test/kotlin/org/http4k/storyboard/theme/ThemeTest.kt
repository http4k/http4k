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
            equalTo("radial-gradient(circle at 82% -40%, rgba(97, 192, 255, 0.10), transparent 60%), url(\"https://http4k.org/images/pipes-hero.svg\") no-repeat left top / cover, #0B1622")
        )
        assertThat(t.accentColor, equalTo("#EE7D2B"))
        assertThat(t.linkColor, equalTo("#0f6cb8"))
        assertThat(t.textColor, equalTo("#1d2433"))
        assertThat(t.textMutedColor, equalTo("#5a6577"))
        assertThat(t.borderColor, equalTo("#E2E8F1"))
        assertThat(t.bgLight, equalTo("#F5F8FC"))
        assertThat(t.bgHover, equalTo("#E9EEF5"))
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
