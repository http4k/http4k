package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.templates.TemplateRenderer
import org.http4k.templates.ViewNotFound
import org.junit.Test


class HandlebarsTemplatesTest {

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `caching classpath`() {
        val renderer = HandlebarsTemplates().CachingClasspath()
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `caching file-based`() {
        val renderer = HandlebarsTemplates().Caching("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `hot reload`() {
        val renderer = HandlebarsTemplates().HotReload("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    private fun checkOnClasspath(renderer: TemplateRenderer) {
        assertThat(renderer(OnClasspath(items)), equalTo("Name:item1Price:£1Feature:prettyName:item2Price:£3Feature:nasty"))
    }

    private fun checkAtRoot(renderer: TemplateRenderer) {
        assertThat(renderer(AtRoot(items)), equalTo("AtRootName:item1Price:£1Feature:prettyAtRootName:item2Price:£3Feature:nasty"))
    }

    private fun checkNonExistent(renderer: TemplateRenderer) {
        assertThat({ renderer(NonExistent) } , throws(equalTo(ViewNotFound(NonExistent))))
    }
}