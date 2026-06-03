package org.http4k.template

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import freemarker.template.Template
import freemarker.template.TemplateException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.StringReader
import java.io.StringWriter

data class HtmlFeature(val description: String) : ViewModel {
    override fun template() = super.template() + ".html"
}

data class EscapingFeature(val description: String) : ViewModel {
    override fun template() = super.template() + ".html"
}

class FreemarkerTemplatesTest : TemplatesContract<FreemarkerTemplates>(FreemarkerTemplates(Configuration(VERSION_2_3_34)))

class FreemarkerViewModelTest : ViewModelContract(FreemarkerTemplates(Configuration(VERSION_2_3_34))) {

    @Test
    fun `can override template name to provide format specific stuff`() {
        val renderer = FreemarkerTemplates(Configuration(VERSION_2_3_34)).CachingClasspath()

        assertThat(renderer(HtmlFeature("pretty")), equalTo("<html><span>pretty</span></html>"))
    }

    @Test
    fun `safeConfiguration HTML-escapes interpolated values`() {
        val renderer = FreemarkerTemplates(FreemarkerTemplates.safeConfiguration()).CachingClasspath()

        assertThat(
            renderer(EscapingFeature("<script>alert(1)</script>")),
            equalTo("<span>&lt;script&gt;alert(1)&lt;/script&gt;</span>")
        )
    }

    @Test
    fun `safeConfiguration rejects new builtin on arbitrary classes (FreeMarker SSTI)`() {
        val config = FreemarkerTemplates.safeConfiguration()
        val template = Template(
            "rce",
            StringReader($$"""${"freemarker.template.utility.Execute"?new()("echo pwned")}"""),
            config
        )
        assertThrows<TemplateException> {
            template.process(emptyMap<String, Any>(), StringWriter())
        }
    }
}
