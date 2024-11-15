package org.http4k.connect.lmstudio

import org.http4k.connect.WithRunningFake

class RunningFakeLmStudioTest : LmStudioContract, WithRunningFake(::FakeLmStudio) {
    override val lmStudio = LmStudio.Http(http)
}
