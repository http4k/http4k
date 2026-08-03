/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import org.http4k.ai.mcp.model.MetaField
import org.http4k.ai.mcp.util.auto
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Receipt
import org.http4k.lens.MetaKey

sealed interface MppPaymentCheck {
    data class Required(val challenges: List<org.http4k.connect.mpp.model.Challenge>) : MppPaymentCheck
    data object Free : MppPaymentCheck
}

fun MetaKey.mppCredential() = auto(MetaField<Credential>("org.paymentauth/credential"), MppMoshi)
fun MetaKey.mppReceipt() = auto(MetaField<Receipt>("org.paymentauth/receipt"), MppMoshi)
