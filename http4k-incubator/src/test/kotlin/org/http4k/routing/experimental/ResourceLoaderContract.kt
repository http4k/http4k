package org.http4k.routing.experimental

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.or
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri.Companion.of
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.routing.Router
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.Unmatched
import org.junit.jupiter.api.Test

abstract class ResourceLoaderContract(private val loader: Router) {

    @Test
    fun `loads existing file`() {
        checkContents("mybob.xml", "<xml>content</xml>", APPLICATION_XML)
    }

    @Test
    fun `loads root index file`() {
        checkContents("", "hello from the root index.html", TEXT_HTML)
        checkContents("/", "hello from the root index.html", TEXT_HTML)
    }

    @Test
    open fun `loads embedded index file`() {
        checkContents("org", "hello from the io index.html", TEXT_HTML)
        checkContents("org/", "hello from the io index.html", TEXT_HTML)
    }

    @Test
    fun `loads existing child file`() {
        checkContents("org/index.html", "hello from the io index.html", TEXT_HTML)
    }

    @Test
    fun `missing file`() {
        checkContents("notAFile", null, TEXT_HTML)
    }

    @Test
    fun `does not serve below route`() {
        checkContents("../../../../.java-version", null, TEXT_HTML)
    }

    protected fun checkContents(path: String, expected: String?, expectedContentType: ContentType) {
        val request = Request(GET, of(path))
        if (expected == null)
            assertThat(loader.match(request), isA<Unmatched>())
        else {
            val response = loader.match(request).matchOrExplode().invoke(request)
            assertThat(response, hasBody(expected))
            assertThat(response, hasHeader("Content-Length", expected.length.toString()) or hasHeader("Content-Length", absent()))
            assertThat(response, hasHeader("Content-Type", expectedContentType.withNoDirectives().toHeaderValue()))
        }
    }

    private fun RouterMatch.matchOrExplode(): HttpHandler = when (this) {
        is MatchingHandler -> this
        else -> error("Unmatched, got $this")
    }
}
