package org.reekwest.http.templates

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.templates.HandlebarsTemplates.Caching
import org.reekwest.http.templates.HandlebarsTemplates.CachingClasspath
import org.reekwest.http.templates.HandlebarsTemplates.HotReload


class HandlebarsTemplatesTest {

    private val items = listOf(
        Item("item1", "£1", listOf(Feature("pretty"))),
        Item("item2", "£3", listOf(Feature("nasty"))))

    @Test
    fun `caching classpath`() {
        val renderer = CachingClasspath()
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `caching file-based`() {
        val renderer = Caching("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `hot reload`() {
        val renderer = HotReload("src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    private fun checkOnClasspath(renderer: TemplateRenderer) {
        assertThat(renderer.toBody(OnClasspath(items)), equalTo("Name:item1Price:£1Feature:prettyName:item2Price:£3Feature:nasty"))
    }

    private fun checkAtRoot(renderer: TemplateRenderer) {
        assertThat(renderer.toBody(AtRoot(items)), equalTo("AtRootName:item1Price:£1Feature:prettyAtRootName:item2Price:£3Feature:nasty"))
    }

    private fun checkNonExistent(renderer: TemplateRenderer) {
        assertThat({ renderer.toBody(NonExistent) } , throws(equalTo(ViewNotFound(NonExistent))))
    }
}