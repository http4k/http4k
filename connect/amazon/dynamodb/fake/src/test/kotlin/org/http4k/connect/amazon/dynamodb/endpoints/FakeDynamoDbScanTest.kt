package org.http4k.connect.amazon.dynamodb.endpoints

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.failureOrNull
import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource
import org.http4k.connect.amazon.dynamodb.attrN
import org.http4k.connect.amazon.dynamodb.attrS
import org.http4k.connect.amazon.dynamodb.createTable
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.sample
import org.http4k.connect.amazon.dynamodb.scan
import org.junit.jupiter.api.Test

class FakeDynamoDbScanTest : DynamoDbScanContract(), DynamoDbSource by FakeDynamoDbSource() {

    /** The shared contract pins only the status and the error type - the wording is the fake's own. */
    @Test
    fun `undefined filter expression value is named in the validation error`() {
        val empty = TableName.sample()
        dynamo.createTable(empty, attrS)

        assertThat(
            dynamo.scan(TableName = empty, FilterExpression = "$attrN = :missing").failureOrNull()?.message,
            equalTo("""{"__type":"com.amazon.coral.validate#ValidationException","Message":"Invalid FilterExpression: An expression attribute value used in expression is not defined; attribute value: :missing"}""")
        )
    }
}
