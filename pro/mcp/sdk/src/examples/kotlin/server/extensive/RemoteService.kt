package server.extensive

import dev.forkhandles.result4k.Result4k
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.time.Clock

interface RemoteService {
    fun doSomething(): Result4k<Int, Exception>

    companion object {
        fun Http(uri: Uri, http: HttpHandler, clock: Clock): RemoteService = TODO()
    }
}
