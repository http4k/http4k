package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.http4k.server.ServerConfig.StopMode
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor

fun Jetty11Loom(port: Int) = Jetty11Loom(port, defaultStopMode)

fun Jetty11Loom(port: Int, stopMode: StopMode) = Jetty11(port, stopMode,
    Server(QueuedThreadPool().apply {
        virtualThreadsExecutor = newVirtualThreadPerTaskExecutor();
    }).apply { addConnector(http(port)(this)) }
)
