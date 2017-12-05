package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

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
        assertThat({noFinds.then(noFinds)(TestViewModel)} , throws<ViewNotFound>())
    }

}