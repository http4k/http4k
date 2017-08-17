package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test


abstract class TemplatesContract(private val templates: Templates) {

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `caching classpath`() {
        val renderer = templates.CachingClasspath()
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `caching classpath not at root`() {
        val renderer = templates.CachingClasspath("org.http4k.template")
        assertThat(renderer(OnClasspathNotAtRoot(items)), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
    }

    @Test
    fun `caching file-based`() {
        val renderer = templates.Caching("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `hot reload`() {
        val renderer = templates.HotReload("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    private fun checkOnClasspath(renderer: TemplateRenderer) {
        assertThat(renderer(OnClasspath(items)), equalTo("<ul><li>Name:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>Name:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty</span></li></ul></li></ul>"))
    }

    private fun checkAtRoot(renderer: TemplateRenderer) {
        assertThat(renderer(AtRoot(items)), equalTo("<ul><li>AtRootName:<span>item1</span>Price:<span>£1</span><ul><li>Feature:<span>pretty</span></li></ul></li><li>AtRootName:<span>item2</span>Price:<span>£3</span><ul><li>Feature:<span>nasty" +
            "</span></li></ul></li></ul>"))
    }

    private fun checkNonExistent(renderer: TemplateRenderer) {
        assertThat({ renderer(NonExistent) } , throws(equalTo(ViewNotFound(NonExistent))))
    }
}