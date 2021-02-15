package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class HandlebarsTemplatesTest : TemplatesContract<HandlebarsTemplates>(HandlebarsTemplates()) {

    @Test
    fun `hot reload multiple loaders`() {
        val renderer = templates.HotReload("src/test/resources/a", "src/test/resources/b")
        assertThat(renderer(TemplateA), equalTo("a"))
        assertThat(renderer(TemplateB), equalTo("b"))
        assertThat(renderer(TemplateC), equalTo("c1"))
    }

    @Test
    fun `multiple loaders, first loaded wins`() {
        val rendererBFirst = templates.HotReload("src/test/resources/b", "src/test/resources/a")
        assertThat(rendererBFirst(TemplateC), equalTo("c2"))
    }

}

class HandlebarsViewModelTest : ViewModelContract(HandlebarsTemplates())
