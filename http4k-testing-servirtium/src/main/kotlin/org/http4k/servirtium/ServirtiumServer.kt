package org.http4k.servirtium

import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.TrafficFilters
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.servirtium.InteractionControl.Companion.StorageBased
import org.http4k.servirtium.InteractionOptions.Companion.Defaults
import org.http4k.traffic.Replay
import org.http4k.traffic.Servirtium
import org.http4k.traffic.Sink
import org.http4k.traffic.replayingMatchingContent

interface ServirtiumServer : Http4kServer, InteractionControl {

    companion object {
        /**
         * Replay server which will match and replay recorded traffic read from the named Servirtium Markdown file.
         * Incoming requests can be manipulated to ensure that it matches the expected request.
         */
        @JvmStatic
        fun Replay(
            name: String,
            storageProvider: StorageProvider,
            options: InteractionOptions = Defaults,
            port: Int = 0,
            serverFn: (Int) -> ServerConfig = ::SunHttp
        ): ServirtiumServer = object : ServirtiumServer,
            Http4kServer by
            options.trafficPrinter()
                .then(Replay.Servirtium(storageProvider(name))
                    .replayingMatchingContent(options::modify))
                .asServer(serverFn(port)),
            InteractionControl by InteractionControl.Companion.NoOp {}

        /**
         * MiTM proxy server which sits in between the client and the target and stores traffic in the
         * named Servirtium Markdown file.
         *
         * Manipulations can be made to the requests and responses before they are stored.
         */
        @JvmStatic
        fun Recording(
            name: String,
            target: Uri,
            storageProvider: StorageProvider,
            options: InteractionOptions = Defaults,
            port: Int = 0,
            serverFn: (Int) -> ServerConfig = ::SunHttp,
            proxyClient: HttpHandler = ApacheClient()
        ): ServirtiumServer {
            val storage = storageProvider(name).apply { clean() }
            return object : ServirtiumServer,
                Http4kServer by
                TrafficFilters.RecordTo(
                    Sink.Servirtium(storage, options))
                    .then(ClientFilters.SetBaseUriFrom(target))
                    .then(options.trafficPrinter())
                    .then(proxyClient)
                    .asServer(serverFn(port)),
                InteractionControl by StorageBased(storage) {
            }
        }
    }
}
