package org.http4k.client

import org.http4k.server.SunHttp
import java.time.Duration

class FuelAsyncTest : AsyncHttpHandlerContract(::SunHttp, Fuel(), Fuel(timeout = Duration.ofMillis(100)))
