package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.UpdateTimeToLive
import org.http4k.connect.amazon.dynamodb.action.UpdateTimeToLiveResponse
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveDescription
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveStatus
import org.http4k.connect.storage.Storage

fun AwsJsonFake.updateTimeToLive(
    tables: Storage<DynamoTable>,
    timeToLive: Storage<TimeToLiveDescription>
) = route<UpdateTimeToLive> { update ->
    tables[update.TableName.value]?.let {
        val spec = update.TimeToLiveSpecification
        // The fake transitions instantly and accepts a redundant enable/disable; real DynamoDB rejects
        // an update within the (up to one hour) processing window with a ValidationException. This matches
        // the fake's general instant-transition simplification (a created table is ACTIVE immediately too).
        timeToLive[update.TableName.value] = if (spec.Enabled) {
            TimeToLiveDescription(TimeToLiveStatus.ENABLED, spec.AttributeName)
        } else {
            // Disabling clears the attribute — DescribeTimeToLive then reports DISABLED with no name,
            // as real DynamoDB does.
            TimeToLiveDescription(TimeToLiveStatus.DISABLED)
        }
        // The UpdateTimeToLive response echoes the requested specification back (Enabled + AttributeName).
        UpdateTimeToLiveResponse(spec)
    }
}
