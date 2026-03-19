package org.http4k.lens

import org.http4k.connect.mpp.MppMoshi.asA
import org.http4k.connect.mpp.MppMoshi.asFormatString
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Receipt
import java.util.Base64

private fun String.base64UrlDecode() = Base64.getUrlDecoder().decode(this).decodeToString()
private fun String.base64UrlEncode() = Base64.getUrlEncoder().withoutPadding().encodeToString(toByteArray())

private const val PAYMENT_SCHEME = "Payment "

val mppChallengeLens = Header
    .map(
        { it.removePrefix(PAYMENT_SCHEME).base64UrlDecode() },
        { PAYMENT_SCHEME + it.base64UrlEncode() }
    )
    .map({ asA<Challenge>(it) }, { asFormatString(it) })
    .required("WWW-Authenticate")

val mppCredentialLens = Header
    .map(
        { it.removePrefix(PAYMENT_SCHEME).base64UrlDecode() },
        { PAYMENT_SCHEME + it.base64UrlEncode() }
    )
    .map({ asA<Credential>(it) }, { asFormatString(it) })
    .optional("Authorization")

val mppReceiptLens = Header
    .map(String::base64UrlDecode, String::base64UrlEncode)
    .map({ asA<Receipt>(it) }, { asFormatString(it) })
    .required("Payment-Receipt")
