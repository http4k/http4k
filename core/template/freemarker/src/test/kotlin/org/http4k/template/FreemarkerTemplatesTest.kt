package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import org.junit.jupiter.api.Test

data class HtmlFeature(val description: String) : ViewModel {
    override fun template() = super.template() + ".html"
}

class FreemarkerTemplatesTest : TemplatesContract<FreemarkerTemplates>(FreemarkerTemplates(Configuration(VERSION_2_3_34)))

class FreemarkerViewModelTest : ViewModelContract(FreemarkerTemplates(Configuration(VERSION_2_3_34))) {

    @Test
    fun `can override template name to provide format specific stuff`() = runBlocking {
        val renderer = FreemarkerTemplates(Configuration(VERSION_2_3_34)).CachingClasspath()

        assertThat(renderer(HtmlFeature("pretty")), equalTo("<html><span>pretty</span></html>"))
    }
}
