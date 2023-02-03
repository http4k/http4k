package org.http4k.server

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.http4k.server.ServerConfig.StopMode
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor

fun JettyLoom(port: Int) = JettyLoom(port, defaultStopMode)

fun JettyLoom(port: Int, stopMode: StopMode) = Jetty(port, stopMode,
    Server(QueuedThreadPool().apply {
        virtualThreadsExecutor = newVirtualThreadPerTaskExecutor(); }
    ).apply { addConnector(http(port)(this)) }
)
