package integration

import org.http4k.client.ApacheClient
import org.http4k.testing.ServerBackend

class ApacheServerStopTest : ServerStopContract(
    ServerBackend.Apache,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    }
)
