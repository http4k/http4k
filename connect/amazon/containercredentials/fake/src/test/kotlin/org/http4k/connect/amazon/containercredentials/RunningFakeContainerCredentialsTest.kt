package org.http4k.connect.amazon.containercredentials

import org.http4k.connect.WithRunningFake
import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.containerCredentials.ContainerCredentialsContract
import org.http4k.core.Uri

class RunningFakeContainerCredentialsTest : ContainerCredentialsContract, FakeAwsContract,
    WithRunningFake(::FakeContainerCredentials) {
    override val fullUri = Uri.of("http://localhost:80/foobar")
}
