/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import org.http4k.template.DatastarElementRenderer
import org.http4k.testing.ApprovalTest
import org.http4k.wiretap.util.Templates
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface HttpWiretapFunctionContract {
    val function: WiretapFunction
    fun httpClient() = function.http(DatastarElementRenderer(Templates()), Templates())
}
