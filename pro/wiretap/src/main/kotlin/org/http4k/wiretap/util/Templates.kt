/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import freemarker.core.HTMLOutputFormat.INSTANCE
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import freemarker.template.TemplateExceptionHandler.IGNORE_HANDLER
import org.http4k.template.FreemarkerTemplates

fun Templates() = FreemarkerTemplates(Configuration(VERSION_2_3_34).apply {
    outputFormat = INSTANCE
    templateExceptionHandler = IGNORE_HANDLER
}).CachingClasspath()
