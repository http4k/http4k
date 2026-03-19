package org.http4k.filter

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.connect.mpp.MppSigner
import org.http4k.core.Filter
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.with
import org.http4k.lens.mppChallengeLens
import org.http4k.lens.mppCredentialLens

fun ClientFilters.MppPaymentRequired(signer: MppSigner) = Filter { next ->
    { req ->
        val response = next(req)
        when (response.status) {
            PAYMENT_REQUIRED -> signer.sign(mppChallengeLens(response))
                .map { next(req.with(mppCredentialLens of it)) }
                .recover { response }

            else -> response
        }
    }
}
