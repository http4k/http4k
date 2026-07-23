package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.TimeToLiveSpecification
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class UpdateTimeToLive(
    val TableName: TableName,
    val TimeToLiveSpecification: TimeToLiveSpecification
) : DynamoDbAction<UpdateTimeToLiveResponse>(UpdateTimeToLiveResponse::class, DynamoDbMoshi)

@JsonSerializable
data class UpdateTimeToLiveResponse(
    val TimeToLiveSpecification: TimeToLiveSpecification
)
