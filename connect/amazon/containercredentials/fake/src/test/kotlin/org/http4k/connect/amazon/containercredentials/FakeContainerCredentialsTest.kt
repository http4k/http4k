package org.http4k.connect.amazon.containercredentials

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.containerCredentials.ContainerCredentialsContract
import org.http4k.core.Uri

class FakeContainerCredentialsTest : ContainerCredentialsContract, FakeAwsContract {
    override val http = FakeContainerCredentials()
    override val fullUri = Uri.of("http://localhost:80/foobar")
}
