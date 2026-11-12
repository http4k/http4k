/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.webdriver.datastar.AttributePlugin
import org.http4k.webdriver.datastar.DatastarAttribute

object Effects : AttributePlugin {
    override val name = "effect"

    private val focus = Regex("""^\s*\$\w+\s*&&\s*el\.focus\(\)\s*;?\s*$""")
    private val fileNames = Regex("""^\s*el\.textContent\s*=\s*\$\w+\.map\(file => file\.name\)\.join\(', '\)\s*;?\s*$""")

    override fun models(attribute: DatastarAttribute) = focus.matches(attribute.expression) || fileNames.matches(attribute.expression)
}
