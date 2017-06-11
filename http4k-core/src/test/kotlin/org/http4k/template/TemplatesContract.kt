package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import java.io.File


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
    fun `caching file-based`() {
        val renderer = templates.Caching("../http4k-core/src/test/resources")
        checkOnClasspath(renderer)
        checkAtRoot(renderer)
        checkNonExistent(renderer)
    }

    @Test
    fun `hot reload`() {
        val renderer = templates.HotReload("../http4k-core/src/test/resources")
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