package org.http4k.routing.experimental

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

class SimpleDirectoryRendererTest {

    @Test
    fun `renders directory name escaped`() {
        val rendered = simpleDirectoryRenderer(
            Uri.of("/"),
            ResourceSummary("<script>alert(1)</script>"),
            emptyList()
        )

        assertThat(rendered, containsSubstring("&lt;script&gt;alert(1)&lt;/script&gt;"))
        assertThat(rendered.contains("<script>alert(1)</script>"), equalTo(false))
    }

    @Test
    fun `renders resource names escaped in link text`() {
        val rendered = simpleDirectoryRenderer(
            Uri.of("/listing"),
            ResourceSummary("dir"),
            listOf(ResourceSummary("\"><img src=x onerror=alert(1)>.txt"))
        )

        assertThat(rendered, containsSubstring("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;.txt"))
        assertThat(rendered.contains("<img src=x onerror=alert(1)>"), equalTo(false))
    }

    @Test
    fun `href percent-encodes resource names`() {
        val rendered = simpleDirectoryRenderer(
            Uri.of("/listing"),
            ResourceSummary("dir"),
            listOf(ResourceSummary("a b?c#d.txt"))
        )

        assertThat(rendered, containsSubstring("""href="/listing/a%20b%3Fc%23d.txt""""))
    }
}
