package org.http4k.connect.amazon.sts

import org.http4k.connect.amazon.RealAwsContract
import java.time.Clock

class RealSTSTest : STSContract, RealAwsContract {
    override val clock: Clock = Clock.systemUTC()
}
