package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DescribeTimeToLive
import org.http4k.connect.amazon.dynamodb.action.TimeToLiveDescriptionResponse
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveDescription
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveStatus
import org.http4k.connect.storage.Storage

// TTL is table-level metadata AWS exposes through its own API (Describe/UpdateTimeToLive), separate from
// DescribeTable, so the fake tracks it in its own Storage (keyed by table name) rather than on DynamoTable.
fun AwsJsonFake.describeTimeToLive(
    tables: Storage<DynamoTable>,
    timeToLive: Storage<TimeToLiveDescription>
) = route<DescribeTimeToLive> { action ->
    // Only respond for a table that exists (else null -> ResourceNotFoundException, matching siblings).
    tables[action.TableName.value]?.let {
        // A table that was never configured reports DISABLED (with no attribute), matching real DynamoDB.
        TimeToLiveDescriptionResponse(
            timeToLive[action.TableName.value] ?: TimeToLiveDescription(TimeToLiveStatus.DISABLED)
        )
    }
}
