package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.ImportTable
import org.http4k.connect.amazon.dynamodb.action.ImportTableResponse
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.ImportStatus
import org.http4k.connect.amazon.dynamodb.model.ImportTableDescription
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import java.time.Clock

fun AwsJsonFake.importTable(
    tables: Storage<DynamoTable>,
    addTableImport: (ImportTableDescription) -> Unit,
    availableS3Buckets: () -> List<FakeS3BucketSource>,
    clock: Clock
) = route<ImportTable> { importRequest ->
    val importDescription =
        availableS3Buckets()
            .firstOrNull { it.name == importRequest.s3Bucket }
            ?.let { bucket ->
                tables[importRequest.tableName] = importRequest.toDynamoTable().withItem(itemFromCsv(bucket.csv))
                ImportTableDescription.completed(importRequest, clock)
            }
            ?: ImportTableDescription.noSuchBucket(importRequest)
    addTableImport(importDescription)
    ImportTableResponse(importDescription)
}

data class FakeS3BucketSource(
    val name: String,
    val csv: String
)

private val ImportTable.tableName get() = TableCreationParameters.TableName.value

private val ImportTable.s3Bucket get() = S3BucketSource.S3Bucket

private fun itemFromCsv(csv: String): Item {
    val lines = csv.lines()
    val headers = lines.first().split(",")
    val row = lines.drop(1).first().split(",")
    return headers.zip(row).associate { (header, value) ->
        AttributeName.of(header) to AttributeValue.Str(value)
    }
}

private fun ImportTable.toDynamoTable() = DynamoTable(
    TableDescription(
        TableName = TableCreationParameters.TableName,
        KeySchema = TableCreationParameters.KeySchema
    ),
    mutableListOf()
)

private fun ImportTableDescription.Companion.noSuchBucket(importRequest: ImportTable): ImportTableDescription {
    val tableArn = ARN.ofTable(importRequest.TableCreationParameters.TableName)
    return ImportTableDescription(
        ClientToken = importRequest.ClientToken,
        TableArn = tableArn,
        ImportArn = importArn(tableArn),
        ImportStatus = ImportStatus.FAILED,
        FailureCode = "S3NoSuchBucket",
        FailureMessage = "The specified bucket does not exist (Service: Amazon S3; Status Code: 404;)"
    )
}

private fun ImportTableDescription.Companion.completed(
    importRequest: ImportTable,
    clock: Clock
): ImportTableDescription {
    val tableArn = ARN.ofTable(importRequest.TableCreationParameters.TableName)
    return ImportTableDescription(
        ClientToken = importRequest.ClientToken,
        CloudWatchLogGroupArn = ARN.cloudwatch,
        StartTime = Timestamp.of(clock.instant()),
        EndTime = Timestamp.of(clock.instant()),
        ErrorCount = 0,
        ImportArn = importArn(tableArn),
        ImportStatus = ImportStatus.COMPLETED,
        InputCompressionType = importRequest.InputCompressionType,
        InputFormat = importRequest.InputFormat,
        InputFormatOptions = importRequest.InputFormatOptions,
        ProcessedItemCount = 1,
        ProcessedSizeBytes = 10,
        S3BucketSource = importRequest.S3BucketSource,
        TableArn = tableArn,
        TableCreationParameters = importRequest.TableCreationParameters,
        TableId = importRequest.TableCreationParameters.TableName.value
    )
}

private fun ARN.Companion.ofTable(tableName: TableName) =
    ARN.of(
        AwsService.of("dynamodb"),
        Region.of("ldn-north-1"),
        AwsAccount.of("0"),
        resourcePath = "table/${tableName.value}"
    )

private val ARN.Companion.cloudwatch
    get() = ARN.of(
        AwsService.of("cloudwatch"),
        Region.of("ldn-north-1"),
        AwsAccount.of("0"),
        resourcePath = "/"
    )

private fun importArn(tableArn: ARN) = ARN.of(tableArn.value + "/import/1234")
