package org.http4k.servirtium

import org.http4k.client.JavaHttpClient
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.TrafficFilters
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.ByteStorage
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import java.io.File

/**
 * MiTM proxy server which sits inbetween the client and the target and stores traffic in the
 * named Servirtium Markdown file.
 *
 * Manipulations can be made to the requests and responses before they are stored.
 */
object ServirtumRecordingServer {
    operator fun invoke(
        name: String,
        target: Uri,
        root: File = File("."),
        port: Int = 0,
        requestManipulations: (Request) -> Request = { it },
        responseManipulations: (Response) -> Response = { it }
    ) =
        TrafficFilters.RecordTo(Sink.Servirtium(ByteStorage.Disk(File(root, "$name.md")), requestManipulations, responseManipulations))
            .then(ClientFilters.SetBaseUriFrom(target))
            .then(JavaHttpClient())
            .asServer(SunHttp(port))
}
