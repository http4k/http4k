/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.TemplateExceptionHandler
import org.http4k.template.FreemarkerTemplates

fun Templates() = FreemarkerTemplates(
    Configuration(Configuration.getVersion()).apply {
        outputFormat = HTMLOutputFormat.INSTANCE
        templateExceptionHandler = TemplateExceptionHandler.IGNORE_HANDLER
    }
).CachingClasspath()
