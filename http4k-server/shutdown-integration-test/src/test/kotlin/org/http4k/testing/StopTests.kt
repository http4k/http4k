package org.http4k.testing

import org.http4k.client.ApacheClient

class ApacheServerStopTest : ServerStopContract(
    ServerBackend.Apache,
    ApacheClient(),
    {
        enableImmediateStop()
        enableGracefulStop()
    }
)
