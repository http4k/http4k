/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.openapi

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.wiretap.HttpWiretapFunctionContract
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class OpenApiTest : HttpWiretapFunctionContract {

    override val function = OpenApi()

    @Test
    fun `http returns openapi index page`(approver: Approver) {
        approver.assertApproved(httpClient()(Request(GET, "/openapi")))
    }
}
