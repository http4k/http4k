package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

data class HtmlFeature(val description: String) : ViewModel {
    override fun template() = super.template() + ".html"
}

class ThymeleafTemplatesTest : TemplatesContract<ThymeleafTemplates>(ThymeleafTemplates())

class ThymeleafViewModelTest : ViewModelContract(ThymeleafTemplates()) {

    @Test
    fun `can override template name to provide format specific stuff`() {
        val renderer = ThymeleafTemplates().CachingClasspath()

        assertThat(renderer(HtmlFeature("pretty")), equalTo("<html><span>pretty</span></html>"))
    }
}

