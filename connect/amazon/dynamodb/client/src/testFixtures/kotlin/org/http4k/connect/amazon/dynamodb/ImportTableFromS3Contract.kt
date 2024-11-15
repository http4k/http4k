package org.http4k.connect.amazon.dynamodb

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThanOrEqualTo
import com.natpryce.hamkrest.present
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.BillingMode
import org.http4k.connect.amazon.dynamodb.model.ClientToken
import org.http4k.connect.amazon.dynamodb.model.CsvOptions
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType
import org.http4k.connect.amazon.dynamodb.model.ImportStatus
import org.http4k.connect.amazon.dynamodb.model.ImportStatus.COMPLETED
import org.http4k.connect.amazon.dynamodb.model.ImportStatus.FAILED
import org.http4k.connect.amazon.dynamodb.model.InputCompressionType.NONE
import org.http4k.connect.amazon.dynamodb.model.InputFormat
import org.http4k.connect.amazon.dynamodb.model.InputFormat.CSV
import org.http4k.connect.amazon.dynamodb.model.InputFormatOptions
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.KeyType
import org.http4k.connect.amazon.dynamodb.model.ProvisionedThroughput
import org.http4k.connect.amazon.dynamodb.model.S3BucketSource
import org.http4k.connect.amazon.dynamodb.model.TableCreationParameters
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.UUID

interface ImportTableFromS3Contract : AwsContract {
    private val dynamo get() = DynamoDb.Http(aws.region, { aws.credentials }, http)

    @Test
    fun `import table is successful`() {
        val table = TableName.sample()
        val bucket = BucketName.sample()
        initBucket(bucket, csv = "ID,AGE\n1,42")
        try {
            val clientToken = ClientToken.random()
            val tableCreationParameters = TableCreationParameters(
                KeySchema = listOf(KeySchema(AttributeName.of("ID"), KeyType.HASH)),
                TableName = table,
                AttributeDefinitions = listOf(
                    AttributeDefinition(
                        AttributeName.of("ID"),
                        AttributeType = DynamoDataType.S
                    )
                ),
                BillingMode = BillingMode.PROVISIONED,
                ProvisionedThroughput = ProvisionedThroughput(ReadCapacityUnits = 5, WriteCapacityUnits = 5)
            )
            val importArn = dynamo.importTable(
                ClientToken = clientToken,
                InputCompressionType = NONE,
                S3BucketSource = S3BucketSource(S3Bucket = bucket.value),
                InputFormat = CSV,
                InputFormatOptions = InputFormatOptions(CsvOptions(Delimiter = ',')),
                TableCreationParameters = tableCreationParameters
            ).successValue().ImportTableDescription.ImportArn!!
            dynamo.waitForImportFinished(importArn, timeout = Duration.ofMinutes(3))

            with(dynamo.describeImport(importArn).successValue().ImportTableDescription) {
                assertThat(ClientToken, equalTo(clientToken))
                assertThat(CloudWatchLogGroupArn, present())
                assertThat(StartTime, present())
                assertThat(EndTime, present())
                assertThat(ErrorCount, equalTo(0))
                assertThat(FailureCode, absent())
                assertThat(FailureMessage, absent())
                assertThat(ImportArn?.awsService, equalTo(AwsService.of("dynamodb")))
                assertThat(ImportStatus, equalTo(COMPLETED))
                assertThat(InputCompressionType, equalTo(NONE))
                assertThat(InputFormat, equalTo(CSV))
                assertThat(InputFormatOptions, equalTo(InputFormatOptions(CsvOptions(Delimiter = ','))))
                assertThat(ProcessedItemCount, present(greaterThanOrEqualTo(0)))
                assertThat(ProcessedSizeBytes, present(greaterThanOrEqualTo(0)))
                assertThat(S3BucketSource?.S3Bucket, equalTo(bucket.value))
                assertThat(TableArn?.awsService, equalTo(AwsService.of("dynamodb")))
                assertThat(TableCreationParameters, equalTo(tableCreationParameters))
                assertThat(TableId, present())
            }
            assertThat(dynamo.getItem(table, key = "ID", value = "1"), present())
        } finally {
            table.delete()
            cleanupBucket(bucket)
        }
    }

    @Test
    fun `import table fails where the S3 bucket does not exist`() {
        val importArn =
            dynamo.importTable(sourceBucket = BucketName.sample()).successValue().ImportTableDescription.ImportArn!!
        dynamo.waitForImportFinished(importArn)

        with(dynamo.describeImport(importArn).successValue().ImportTableDescription) {
            assertThat(ImportStatus, equalTo(FAILED))
            assertThat(FailureCode, present())
            assertThat(FailureMessage, present())
        }
    }

    @Test
    fun `query table imports by table ARN`() {
        val import = dynamo.importTable(sourceBucket = BucketName.sample()).successValue().ImportTableDescription

        val importSummaries = dynamo.listImports(TableArn = import.TableArn!!).successValue().ImportSummaryList

        assertThat(importSummaries.map { it.ImportArn }.contains(import.ImportArn!!), equalTo(true))
    }

    abstract fun initBucket(bucketName: BucketName, csv: String)

    abstract fun cleanupBucket(bucketName: BucketName)

    private fun TableName.delete() {
        val (tables, _) = dynamo.listTables().successValue()
        if (tables.contains(this)) {
            dynamo.deleteTable(this).successValue()
        }
    }
}

private fun DynamoDb.importTable(
    sourceBucket: BucketName,
    inputFormat: InputFormat = CSV,
    tableName: TableName = TableName.sample(),
    key: String = "ID"
) = importTable(
    ClientToken = ClientToken.random(),
    InputCompressionType = NONE,
    S3BucketSource = S3BucketSource(S3Bucket = sourceBucket.value),
    InputFormat = inputFormat,
    InputFormatOptions = InputFormatOptions(CsvOptions(Delimiter = ',')),
    TableCreationParameters = TableCreationParameters(
        KeySchema = listOf(KeySchema(AttributeName.of(key), KeyType.HASH)),
        TableName = tableName,
        AttributeDefinitions = listOf(
            AttributeDefinition(
                AttributeName.of(key),
                AttributeType = DynamoDataType.S
            )
        ),
        BillingMode = BillingMode.PROVISIONED,
        ProvisionedThroughput = ProvisionedThroughput(ReadCapacityUnits = 5, WriteCapacityUnits = 5)
    )
)

private fun DynamoDb.waitForImportFinished(importArn: ARN, timeout: Duration = Duration.ofSeconds(20)) {
    waitUntil(
        { describeImport(importArn).successValue().ImportTableDescription.ImportStatus != ImportStatus.IN_PROGRESS },
        failureMessage = "Import $importArn was not finished after $timeout",
        timeout = timeout
    )
}

private fun DynamoDb.getItem(tableName: TableName, key: String, value: String) =
    getItem(tableName, Key(Attribute.string().required(key) of value)).successValue().item

private fun BucketName.Companion.sample() = BucketName.of("http4k-connect-${UUID.randomUUID()}")
