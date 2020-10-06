package org.http4k.template

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasContentType
import org.http4k.hamkrest.hasStatus
import org.junit.jupiter.api.Test

object TestViewModel : ViewModel

class TemplateRendererTest {

    private val finds = { v: ViewModel -> v.template() }
    private val noFinds = { v: ViewModel -> throw ViewNotFound(v) }

    @Test
    fun `can compose template renderers`() {
        assertThat(noFinds.then(finds)(TestViewModel), equalTo("org/http4k/template/TestViewModel"))
    }

    @Test
    fun `eventually fails with ViewNotFound`() {
        assertThat({ noFinds.then(noFinds)(TestViewModel) }, throws<ViewNotFound>())
    }

    @Test
    fun `can generate response with default code and content type`() {
        assertThat(finds.renderToResponse(TestViewModel), hasStatus(OK).and(hasBody("org/http4k/template/TestViewModel")).and(hasContentType(TEXT_HTML)))
    }
}