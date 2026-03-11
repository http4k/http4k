package org.http4k.lens

import org.http4k.connect.x402.X402Moshi.asA
import org.http4k.connect.x402.X402Moshi.asFormatString
import org.http4k.connect.x402.action.Settled
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequired

val paymentSignatureLens = Header
    .base64()
    .map({ asA<PaymentPayload>(it) }, { asFormatString(it) })
    .optional("X-PAYMENT")

val paymentRequiredLens = Header
    .base64()
    .map({ asA<PaymentRequired>(it) }, { asFormatString(it) })
    .required("X-PAYMENT-REQUIRED")

val paymentResponseLens = Header
    .base64()
    .map({ asA<Settled>(it) }, { asFormatString(it) })
    .required("X-PAYMENT-RESPONSE")
