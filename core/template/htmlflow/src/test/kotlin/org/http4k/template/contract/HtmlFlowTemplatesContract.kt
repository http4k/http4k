package org.http4k.template.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.format.Jackson.pretty
import org.http4k.template.AtRoot
import org.http4k.template.Feature
import org.http4k.template.Item
import org.http4k.template.NonExistent
import org.http4k.template.OnClasspath
import org.http4k.template.OnClasspathNotAtRoot
import org.http4k.template.TemplateRenderer
import org.http4k.template.Templates
import org.http4k.template.ViewModel
import org.http4k.template.ViewNotFound
import org.junit.jupiter.api.Test

abstract class HtmlFlowTemplatesContract<out T : Templates>(protected val templates: T) {

    open val supportsRoot = true

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty")))
    )

    @Test
    fun `caching classpath`() {
        val renderer = templates.CachingClasspath()
        checkOnClasspath(renderer)
        if (supportsRoot) checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `caching classpath not at root`() {
        val renderer = templates.CachingClasspath("org.http4k.template")
        assertThat(
            renderer(onClasspathNotAtRootViewModel(items)).trimHtml(),
            equalTo("<!DOCTYPE html><html><body><ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul></body></html>")
        )
    }

    open fun onClasspathNotAtRootViewModel(items: List<Item>): ViewModel = OnClasspathNotAtRoot(items)

    @Test
    fun `hot reload`() {
        val renderer = templates.HotReload("src/test/kotlin")
        checkOnClasspath(renderer)
        if (supportsRoot) checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    private fun checkOnClasspath(renderer: TemplateRenderer) {
        assertThat(
            renderer(onClasspathViewModel(items)).trimHtml(),
            equalTo("<!DOCTYPE html><html><body><ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul></body></html>")
        )
    }

    open fun onClasspathViewModel(items: List<Item>): ViewModel = OnClasspath(items)

    open fun atRootViewModel(items: List<Item>): ViewModel = AtRoot(items)

    private fun checkAtRoot(renderer: TemplateRenderer) {
        assertThat(
            renderer(atRootViewModel(items)).trimHtml(), equalTo("<!DOCTYPE html><html><body><ul><li>AtRootName:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>AtRootName:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul></body></html>")
        )
    }

    private fun checkNonExistent(renderer: TemplateRenderer) {
        assertThat({ renderer(NonExistent) }, throws(equalTo(ViewNotFound(NonExistent))))
    }

    fun String.trimHtml(): String =
        replace(Regex(">\\s+<"), "><")
            .replace(Regex(">\\s+"), ">")
            .replace(Regex("\\s+<"), "<")
            .trim()
}
