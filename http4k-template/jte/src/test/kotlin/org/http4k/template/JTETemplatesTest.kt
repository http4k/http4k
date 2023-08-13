package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import gg.jte.ContentType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

//@Disabled
class JTETemplatesTest : TemplatesContract<JTETemplates>(JTETemplates(ContentType.Html)) {

    override val supportsRoot = false

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
}

@Disabled
class JTEViewModelTest : ViewModelContract(JTETemplates(ContentType.Html))
