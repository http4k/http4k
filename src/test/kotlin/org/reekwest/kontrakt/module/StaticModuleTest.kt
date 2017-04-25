package org.reekwest.kontrakt.module

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Ignore
import org.junit.Test
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.ContentType.Companion.APPLICATION_XML
import org.reekwest.http.core.ContentType.Companion.TEXT_HTML
import org.reekwest.http.core.Filter
import org.reekwest.http.core.Method.GET
import org.reekwest.http.core.Request
import org.reekwest.http.core.Status.Companion.EXPECTATION_FAILED
import org.reekwest.http.core.Status.Companion.NOT_FOUND
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.core.Uri.Companion.uri
import org.reekwest.http.core.body.bodyString
import org.reekwest.kontrakt.Header.Common.CONTENT_TYPE
import org.reekwest.kontrakt.module.ResourceLoader.Companion.Classpath

class StaticModuleTest {

    private val pkg = this.javaClass.`package`.name.replace('.','/')

    @Test
    fun `looks up contents of existing root file`() {
        val module = StaticModule(Root / "svc")
        val result = module.toHttpHandler()(Request(GET, uri("/svc/mybob.xml")))
        assertThat(result.bodyString(), equalTo("<xml>content</xml>"))
        assertThat(CONTENT_TYPE(result), equalTo(APPLICATION_XML))
    }

    @Test
    fun `defaults to index html if is no route`() {
        val module = StaticModule(Root / "svc")
        val result = module.toHttpHandler()(Request(GET, uri("/svc")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("hello from the root index.html"))
        assertThat(CONTENT_TYPE(result), equalTo(TEXT_HTML))
    }

    @Test
    fun `defaults to index html if is no route - non-root-context`() {
        val module = StaticModule(Root / "svc", Classpath("org"))
        val result = module.toHttpHandler()(Request(GET, uri("/svc")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("hello from the io index.html"))
        assertThat(CONTENT_TYPE(result), equalTo(TEXT_HTML))
    }

    @Test
    fun `non existing index html if is no route`() {
        val module = StaticModule(Root / "svc", Classpath("org/reekwest"))
        val result = module.toHttpHandler()(Request(GET, uri("/svc")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `looks up contents of existing subdir file - non-root context`() {
        val module = StaticModule(Root / "svc")
        val result = module.toHttpHandler()(Request(GET, uri("/svc/$pkg/StaticModule.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }

    @Test
    fun `looks up contents of existing subdir file`() {
        val module = StaticModule(Root)
        val result = module.toHttpHandler()(Request(GET, uri("/$pkg/StaticModule.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }

    @Test
    fun `can alter the root path`() {
        val module = StaticModule(Root / "svc", Classpath(pkg))
        val result = module.toHttpHandler()(Request(GET, uri("/svc/StaticModule.js")))
        assertThat(result.status, equalTo(OK))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }

    @Test
    fun `can add a filter`() {
        val module = StaticModule(Root / "svc", Classpath(pkg), Filter {
            { req -> it(req).copy(EXPECTATION_FAILED) }
        })
        val result = module.toHttpHandler()(Request(GET, uri("/svc/StaticModule.js")))
        assertThat(result.status, equalTo(EXPECTATION_FAILED))
        assertThat(result.bodyString(), equalTo("function hearMeNow() { }"))
        assertThat(CONTENT_TYPE(result), equalTo(ContentType("application/javascript")))
    }

    @Test
    fun `looks up non existent-file`() {
        val module = StaticModule(Root / "svc", Classpath())
        val result = module.toHttpHandler()(Request(GET, uri("/svc/NotHere.xml")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    @Ignore // FIXME what to go in this case..
    fun `cannot serve the root`() {
        val module = StaticModule(Root / "svc", Classpath())
        val result = module.toHttpHandler()(Request(GET, uri("/")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `looks up non existent path`() {
        val module = StaticModule(Root / "svc")
        val result = module.toHttpHandler()(Request(GET, uri("/bob/StaticModule.js")))
        assertThat(result.status, equalTo(NOT_FOUND))
    }

    @Test
    fun `can't subvert the path`() {
        val module = StaticModule(Root / "svc")
        assertThat(module.toHttpHandler()(Request(GET, uri("/svc/../svc/Bob.xml"))).status, equalTo(NOT_FOUND))
        assertThat(module.toHttpHandler()(Request(GET, uri("/svc/~/.bashrc"))).status, equalTo(NOT_FOUND))
    }
}
