package org.http4k.storyboard.util

import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.http4k.template.FreemarkerTemplates
import org.http4k.template.TemplateRenderer

fun StoryboardTemplates(): TemplateRenderer = FreemarkerTemplates(Configuration(Configuration.VERSION_2_3_34).apply {
    outputFormat = HTMLOutputFormat.INSTANCE
    templateExceptionHandler = TemplateExceptionHandler.IGNORE_HANDLER
}).CachingClasspath()
