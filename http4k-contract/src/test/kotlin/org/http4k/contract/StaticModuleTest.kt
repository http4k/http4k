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
import org.http4k.core.Uri.Companion.uri
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.junit.Test

class StaticModuleTest {

    private val pkg = this::class.java.`package`.name.replace('.', '/')

    @Test
    fun `looks up contents of existing root file`() {
        val router = StaticModule(Root / "svc").toRouter()
        val request = Request(GET, uri("/svc/mybob.xml"))
        val result = router(request)!!(request)
        assertThat(result.bodyString(), equalTo("<xml>content</xml>"))
        assertThat(CONTENT_TYPE(result), equalTo(APPLICATION_XML))
    }

    @Test
    fun `looks up non existent-file`() {
        assertThat((StaticModule(Root / "svc", Classpath()).toRouter())(Request(GET, uri("/svc/NotHere.xml"))), absent())
    }

    @Test
    fun `can add a filter`() {
        val handler = StaticModule(Root / "svc", Classpath(pkg), Filter.Companion {
            next ->
            { next(it).header("foo", "bar") }
        }).toHttpHandler()

        val result = handler(Request(GET, uri("/svc/StaticModule.js")))
        assertThat(result.header("foo"), equalTo("bar"))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }
}
