package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.datastar.Element
import org.junit.jupiter.api.Test

class DatastarElementRendererTest {

    @Test
    fun `renders template into a merge element event`() {
        val viewModel1 = object : ViewModel {
            override fun toString() = """<foo>bar</foo>
        |<bar>foo</bar>
    """.trimMargin()
        }

        val viewModel2 = object : ViewModel {
            override fun toString() = """<foo2>bar</foo2>
        |<bar2>foo</bar2>
    """.trimMargin()
        }

        val renderer = DatastarElementRenderer(object : TemplateRenderer {
            override fun invoke(p1: ViewModel) = p1.toString()
        })

        assertThat(
            renderer(viewModel1, viewModel2),
            equalTo(
                listOf(Element.of("<foo>bar</foo><bar>foo</bar>"), Element.of("<foo2>bar</foo2><bar2>foo</bar2>"))
            )
        )
    }

}
