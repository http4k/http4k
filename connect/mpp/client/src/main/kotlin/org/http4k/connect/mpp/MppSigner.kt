package org.http4k.connect.mpp

import dev.forkhandles.result4k.Result
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.Credential

fun interface MppSigner {
    fun sign(challenge: Challenge): Result<Credential, String>
}
