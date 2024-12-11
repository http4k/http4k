package org.http4k.connect.amazon.dynamodb.events

import com.amazonaws.services.lambda.runtime.Context
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.mock4k.mock
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.events.EventName.MODIFY
import org.http4k.connect.amazon.dynamodb.events.StreamViewType.NEW_AND_OLD_IMAGES
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.serverless.FnHandler
import org.http4k.serverless.FnLoader
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class DynamoEventsTest: PortBasedTest {

    private val event = DynamoDbEvent(
        listOf(
            StreamRecord(
                "id", MODIFY, "version", "source", Region.of("us-east-1"),
                Dynamodb(
                    Item(Attribute.boolean().required("theBool") of true),
                    Item(Attribute.int().required("theInt") of 123),
                    Item(Attribute.string().required("theString") of "hello"),
                    "123",
                    123,
                    NEW_AND_OLD_IMAGES
                ),
                ARN.of("arn:aws:sts:us-east-1:000000000001:role:myrole")
            )
        )
    )

    @Test
    fun `can roundtrip event`(approver: Approver) {
        val json = DynamoDbMoshi.asFormatString(event)
        approver.assertApproved(json)
        assertThat(DynamoDbMoshi.asA(json), equalTo(event))
    }

    @Test
    fun `can launch a custom function`() {
        val fnLoader = FnLoader(DynamoDbMoshi) {
            FnHandler { e: DynamoDbEvent, _: Context ->
                e.Records!![0].eventSourceARN!!
            }
        }

        val result = fnLoader(emptyMap())(DynamoDbMoshi.asInputStream(event), mock())

        assertThat(result.reader().readText(), equalTo(event.Records?.get(0)?.eventSourceARN?.value))
    }
}
