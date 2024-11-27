package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ImportSummary
import org.http4k.connect.amazon.dynamodb.model.NextToken
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListImports(
    val NextToken: NextToken? = null,
    val PageSize: Int? = null,
    val TableArn: ARN? = null
) : DynamoDbAction<ListImportsResponse>(ListImportsResponse::class, DynamoDbMoshi)

@JsonSerializable
data class ListImportsResponse(
    val ImportSummaryList: List<ImportSummary>,
    val NextToken: NextToken? = null
)
