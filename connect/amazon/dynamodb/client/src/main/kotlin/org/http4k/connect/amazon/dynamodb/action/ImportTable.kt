package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.ClientToken
import org.http4k.connect.amazon.dynamodb.model.ImportTableDescription
import org.http4k.connect.amazon.dynamodb.model.InputCompressionType
import org.http4k.connect.amazon.dynamodb.model.InputFormat
import org.http4k.connect.amazon.dynamodb.model.InputFormatOptions
import org.http4k.connect.amazon.dynamodb.model.S3BucketSource
import org.http4k.connect.amazon.dynamodb.model.TableCreationParameters
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ImportTable(
    val InputFormat: InputFormat,
    val S3BucketSource: S3BucketSource,
    val TableCreationParameters: TableCreationParameters,
    val ClientToken: ClientToken = org.http4k.connect.amazon.dynamodb.model.ClientToken.random(),
    val InputCompressionType: InputCompressionType? = null,
    val InputFormatOptions: InputFormatOptions? = null
) : DynamoDbAction<ImportTableResponse>(ImportTableResponse::class, DynamoDbMoshi)

@JsonSerializable
data class ImportTableResponse(
    val ImportTableDescription: ImportTableDescription,
)
