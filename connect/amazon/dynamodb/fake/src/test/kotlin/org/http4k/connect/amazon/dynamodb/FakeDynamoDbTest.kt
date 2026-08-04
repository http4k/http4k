package org.http4k.connect.amazon.dynamodb

import org.http4k.connect.amazon.FakeAwsContract
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.junit.jupiter.api.Disabled
import java.time.Duration
import java.time.Duration.ZERO

class FakeDynamoDbTest : DynamoDbContract, FakeAwsContract {
    override val table = TableName.sample()
    override val http = FakeDynamoDb()
    override val duration: Duration get() = ZERO

    @Disabled
    override fun `partiSQL operations`() {
    }
}
