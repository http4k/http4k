package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.template.contract.HtmlFlowTemplatesContract
import org.http4k.template.contract.HtmlFlowViewModelContract
import org.junit.jupiter.api.Test

class HtmlFlowTemplatesTest : HtmlFlowTemplatesContract<HtmlFlowTemplates>(HtmlFlowTemplates()) {
    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty")))
    )

    @Test
    fun `hot reload classpath`(){
        val renderer = templates.HotReloadClasspath()
        assertOnClasspath(renderer)
    }

    @Test
    fun `renderer extension function`() {
        val renderer = onClassPath.renderer()
        assertOnClasspath(renderer)
        val notAtRootViewModel = onClasspathNotAtRootViewModel(items)
        assertThat(
            { renderer(notAtRootViewModel) },
            throws(equalTo(ViewNotFound(notAtRootViewModel)))
        )
    }

    fun assertOnClasspath(renderer: TemplateRenderer) {
        assertThat(
            renderer(onClasspathViewModel(items)).trimHtml(),
            equalTo("<!DOCTYPE html><html><body><ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul></body></html>")
        )
    }
}

class HtmlFlowViewModelTest : HtmlFlowViewModelContract(HtmlFlowTemplates())
