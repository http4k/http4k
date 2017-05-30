package org.http4k.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.ResourceLoader.Companion.Classpath
import org.http4k.core.Status
import org.http4k.core.Uri.Companion.of
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.junit.Test

class StaticRouterTest {

    private val pkg = this::class.java.`package`.name.replace('.', '/')

    @Test
    fun `looks up contents of existing root file`() {
        val router = StaticRouter(Root / "svc")
        val request = Request(GET, of("/svc/mybob.xml"))
        val result = router.match(request)!!(request)
        assertThat(result.bodyString(), equalTo("<xml>content</xml>"))
        assertThat(CONTENT_TYPE(result), equalTo(APPLICATION_XML))
    }

    @Test
    fun `can register custom mime types`() {
        val router = StaticRouter(Root / "svc", Classpath(), extraPairs = "myxml" to APPLICATION_XML)
        val request = Request(GET, of("/svc/mybob.myxml"))
        val result = router.match(request)!!(request)
        assertThat(result.status, equalTo(Status.OK))
        assertThat(result.bodyString(), equalTo("<myxml>content</myxml>"))
        assertThat(result.header("Content-Type"), equalTo(APPLICATION_XML.value))
    }

    @Test
    fun `looks up non existent-file`() {
        assertThat(StaticRouter(Root / "svc", Classpath()).match(Request(GET, of("/svc/NotHere.xml"))), absent())
    }

    @Test
    fun `can add a filter`() {
        val handler = StaticRouter(Root / "svc", Classpath(pkg), Filter.Companion {
            next ->
            { next(it).header("foo", "bar") }
        }).toHttpHandler()

        val result = handler(Request(GET, of("/svc/StaticModule.js")))
        assertThat(result.header("foo"), equalTo("bar"))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }
}
