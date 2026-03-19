package org.http4k.connect.mpp

import dev.forkhandles.result4k.Result
import org.http4k.connect.RemoteFailure
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Receipt

fun interface MppVerifier {
    fun verify(credential: Credential): Result<Receipt, RemoteFailure>
}
