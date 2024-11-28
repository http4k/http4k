package org.http4k.stream

import org.http4k.core.BodyMode.Stream
import org.http4k.server.ClientForServerTesting
import org.http4k.server.KtorCIO
import org.http4k.streaming.StreamingContract
import org.http4k.streaming.StreamingTestConfiguration
import java.util.*

class KtorCIOStreamingTest : StreamingContract(
    StreamingTestConfiguration(multiplier = 4)
) {

    override fun serverConfig() = KtorCIO(Random().nextInt(1000) + 10000)

    override fun createClient() = ClientForServerTesting(bodyMode = Stream)
}
