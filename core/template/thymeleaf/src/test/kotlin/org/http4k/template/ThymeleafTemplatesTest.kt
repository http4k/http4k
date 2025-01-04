package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

data class CssFeature(val color: String) : ViewModel {
    override fun template() = super.template() + ".css"
}

data object WithFragment : ViewModel {
    override fun template() = super.template() + "::the-fragment"
}

class ThymeleafTemplatesTest : TemplatesContract<ThymeleafTemplates>(ThymeleafTemplates())

class ThymeleafViewModelTest : ViewModelContract(ThymeleafTemplates()) {

    @Test
    fun `can override template name to provide format specific stuff`() {
        val renderer = ThymeleafTemplates().CachingClasspath()

        assertThat(renderer(CssFeature("blue")), equalTo("body {\n" +
                "  background-color: blue;\n" +
                "}\n"))
    }

    @Test
    fun `can specify template fragment`() {
        val renderer = ThymeleafTemplates().CachingClasspath()

        assertThat(renderer(WithFragment), equalTo("<span>inside</span>"))
    }
}
