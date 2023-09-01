package org.http4k.server

import org.http4k.server.ServerConfig.StopMode.Graceful
import java.time.Duration.ofSeconds

internal val defaultStopMode = Graceful(ofSeconds(5))
