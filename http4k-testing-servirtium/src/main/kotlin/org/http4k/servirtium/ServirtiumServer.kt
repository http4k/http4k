package org.http4k.servirtium

import org.http4k.client.JavaHttpClient
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.TrafficFilters
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.ByteStorage
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.http4k.traffic.replayingMatchingContent
import java.io.File

interface ServirtiumServer : Http4kServer, RecordingControl {

    companion object {
        /**
         * Replay server which will match and replay recorded traffic read from the named Servirtium Markdown file.
         * Incoming requests can be manipulated to ensure that it matches the expected request.
         */
        fun Replay(
            name: String,
            root: File = File("."),
            port: Int = 0,
            requestManipulations: (Request) -> Request = { it }
        ): ServirtiumServer {
            return object : ServirtiumServer, Http4kServer by
            Replay.Servirtium(ByteStorage.Disk(File(root, "$name.md")))
                .replayingMatchingContent(requestManipulations)
                .asServer(SunHttp(port)),
                RecordingControl by RecordingControl.Companion.NoOp {}
        }

        /**
         * MiTM proxy server which sits inbetween the client and the target and stores traffic in the
         * named Servirtium Markdown file.
         *
         * Manipulations can be made to the requests and responses before they are stored.
         */
        fun Recording(
            name: String,
            target: Uri,
            root: File = File("."),
            port: Int = 0,
            requestManipulations: (Request) -> Request = { it },
            responseManipulations: (Response) -> Response = { it }
        ): ServirtiumServer = object : ServirtiumServer, Http4kServer by
        TrafficFilters.RecordTo(
            Sink.Servirtium(
                ByteStorage.Disk(File(root, "$name.md")), requestManipulations, responseManipulations))
            .then(ClientFilters.SetBaseUriFrom(target))
            .then(JavaHttpClient())
            .asServer(SunHttp(port)),
            RecordingControl by RecordingControl.ByteStorage(ByteStorage.Disk(File(root, "$name.md"))) {

        }
    }
}
