package org.http4k.connect.amazon.firehose

import org.http4k.connect.amazon.RealAwsContract
import org.junit.jupiter.api.Disabled

@Disabled("This requires other AWS resources which we do not have")
class RealFirehoseTest : FirehoseContract, RealAwsContract
