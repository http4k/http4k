/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import freemarker.cache.ClassTemplateLoader
import freemarker.core.HTMLOutputFormat.INSTANCE
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import freemarker.template.TemplateExceptionHandler.IGNORE_HANDLER
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import java.io.StringWriter

fun Templates(): TemplateRenderer {
    val cfg = Configuration(VERSION_2_3_34).apply {
        outputFormat = INSTANCE
        templateExceptionHandler = IGNORE_HANDLER
        templateLoader = ClassTemplateLoader(ClassLoader.getSystemClassLoader(), "")
    }
    return { viewModel: ViewModel ->
        val writer = StringWriter()
        cfg.getTemplate(viewModel.template() + ".ftl.html").process(viewModel, writer)
        writer.toString()
    }
}
