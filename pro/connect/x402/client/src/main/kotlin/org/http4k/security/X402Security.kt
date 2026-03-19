/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security

import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.core.Request
import org.http4k.filter.ServerFilters
import org.http4k.filter.X402PaymentRequired

class X402Security(
    requirements: (Request) -> List<PaymentRequirements>,
    facilitator: X402Facilitator
) : Security {
    override val filter = ServerFilters.X402PaymentRequired(facilitator, requirements)
}
