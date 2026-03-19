/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security

import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.core.Request
import org.http4k.filter.MppPaymentRequired
import org.http4k.filter.ServerFilters

class MppSecurity(
    challengeFor: (Request) -> Challenge,
    verifier: MppVerifier
) : Security {
    override val filter = ServerFilters.MppPaymentRequired(verifier, challengeFor)
}
