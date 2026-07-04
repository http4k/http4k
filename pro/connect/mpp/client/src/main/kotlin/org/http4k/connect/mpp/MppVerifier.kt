/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.mpp

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Receipt

fun interface MppVerifier {
    /**
     * @param challenge the server-issued challenge for this request. Implementations should bind the
     * settlement to it (e.g. amount/intent) so a credential minted for a different intent cannot settle.
     */
    fun verify(challenge: Challenge, credential: Credential): Result<Receipt, RemoteFailure>
}
