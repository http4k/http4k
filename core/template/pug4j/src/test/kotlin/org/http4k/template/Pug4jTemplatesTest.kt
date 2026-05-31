package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class Pug4JTemplatesTest : TemplatesContract<Pug4jTemplates>(Pug4jTemplates()) {
    @Test
    fun `hot reload multiple loaders`() {
        val renderer = templates.HotReload("src/test/resources/a")
        assertThat(renderer(TemplateA), equalTo("a"))
        assertThat(renderer(TemplateC), equalTo("c1"))
    }

    @Test
    fun `multiple loaders, first loaded wins`() {
        val rendererBFirst = templates.HotReload("src/test/resources/b")
        assertThat(rendererBFirst(TemplateC), equalTo("c2"))
    }

    @Test
    fun `hot reload rejects template path that escapes base dir`() {
        val renderer = templates.HotReload("src/test/resources/a")
        val escaping = object : ViewModel {
            override fun template() = "../AtRootBob"
        }
        assertThrows<ViewNotFound> { renderer(escaping) }
    }
}

class Pug4jViewModelTest : ViewModelContract(Pug4jTemplates())
