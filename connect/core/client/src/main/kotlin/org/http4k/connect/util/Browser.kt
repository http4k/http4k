package org.http4k.connect.util

import dev.forkhandles.result4k.Result4k
import org.http4k.core.Uri

interface Browser {
    fun navigateTo(url: Uri): Result4k<Unit, Exception>
}
