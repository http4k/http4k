package org.http4k.connect.amazon

import java.util.UUID

interface FakeAwsContract : AwsContract {
    override val aws: AwsEnvironment get() = fakeAwsEnvironment

    override fun uuid(seed: Int) = UUID(seed.toLong(), seed.toLong())
}

