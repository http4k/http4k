package org.http4k.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.http4k.http.core.ContentType
import org.http4k.http.core.ContentType.Companion.APPLICATION_XML
import org.http4k.http.core.Filter
import org.http4k.http.core.Method.GET
import org.http4k.http.core.Request
import org.http4k.http.core.ResourceLoader.Companion.Classpath
import org.http4k.http.core.Status.Companion.EXPECTATION_FAILED
import org.http4k.http.core.Uri.Companion.uri
import org.http4k.http.lens.Header.Common.CONTENT_TYPE

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
            next -> { next(it).copy(EXPECTATION_FAILED) }
        }).toHttpHandler()

        val result = handler(Request(GET, uri("/svc/StaticModule.js")))
        assertThat(result.status, equalTo(EXPECTATION_FAILED))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }
}
