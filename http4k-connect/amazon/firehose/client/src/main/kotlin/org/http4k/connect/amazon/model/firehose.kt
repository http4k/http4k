package org.http4k.connect.amazon.model

import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.minLength
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.ResourceId
import org.http4k.connect.model.Base64Blob
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

class DeliveryStreamName private constructor(value: String) : ResourceId(value) {
    companion object : StringValueFactory<DeliveryStreamName>(::DeliveryStreamName, 1.minLength)
}

@JsonSerializable
data class Record(
    val Data: Base64Blob
)

@JsonSerializable
enum class KeyType {
    AWS_OWNED_CMK, CUSTOMER_MANAGED_CMK
}

@JsonSerializable
enum class ParameterName {
    LambdaArn, NumberOfRetries, RoleArn, BufferSizeInMBs, BufferIntervalInSeconds
}

@JsonSerializable
data class DeliveryStreamEncryptionConfigurationInput(
    val KeyType: KeyType,
    val KeyARN: ARN? = null
)

@JsonSerializable
data class ProcessorParameter(
    val ParameterName: ParameterName,
    val ParameterValue: String
)

@JsonSerializable
data class Processor(
    val Parameters: List<ProcessorParameter>? = null
) {
    val Type: String = "Lambda"
}

@JsonSerializable
data class VpcConfiguration(
    val RoleARN: ARN,
    val SecurityGroupIds: List<String>,
    val SubnetIds: List<String>
)

@JsonSerializable
enum class IndexRotationPeriod {
    NoRotation, OneHour, OneDay, OneWeek, OneMonth
}

@JsonSerializable
enum class ElasticS3BackupMode {
    FailedDocumentsOnly, AllDocuments
}

@JsonSerializable
data class ElasticsearchDestinationConfiguration(
    val RoleARN: ARN,
    val IndexName: String,
    val S3Configuration: S3DestinationConfiguration,
    val BufferingHints: BufferingHints? = null,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val ClusterEndpoint: String? = null,
    val DomainARN: ARN? = null,
    val IndexRotationPeriod: IndexRotationPeriod? = null,
    val ProcessingConfiguration: ProcessingConfiguration? = null,
    val RetryOptions: RetryOptions? = null,
    val S3BackupMode: ElasticS3BackupMode? = null,
    val TypeName: String? = null,
    val VpcConfiguration: VpcConfiguration? = null
)

@JsonSerializable
data class HiveJsonSerDe(
    val TimestampFormats: List<String>? = null
)

@JsonSerializable
data class OpenXJsonSerDe(
    val CaseInsensitive: Boolean? = null,
    val ColumnToJsonKeyMappings: Map<String, String>? = null,
    val ConvertDotsInJsonKeysToUnderscores: Boolean? = null
)

@JsonSerializable
data class Deserializer(
    val HiveJsonSerDe: HiveJsonSerDe? = null,
    val OpenXJsonSerDe: OpenXJsonSerDe? = null
)

@JsonSerializable
data class InputFormatConfiguration(
    val Deserializer: Deserializer? = null
)

@JsonSerializable
enum class Compression {
    NONE, UNCOMPRESSED, ZLIB, SNAPPY
}

@JsonSerializable
enum class FormatVersion {
    V0_11, V0_12
}

@JsonSerializable
enum class WriterVersion {
    V1, V2
}

@JsonSerializable
data class OrcSerDe(
    val BlockSizeBytes: Int? = null,
    val BloomFilterColumns: List<String>? = null,
    val BloomFilterFalsePositiveProbability: Int? = null,
    val Compression: Compression? = null,
    val DictionaryKeyThreshold: Int? = null,
    val EnablePadding: Boolean? = null,
    val FormatVersion: FormatVersion? = null,
    val PaddingTolerance: Int? = null,
    val RowIndexStride: Int? = null,
    val StripeSizeBytes: Int? = null
)

@JsonSerializable
data class ParquetSerDe(
    val BlockSizeBytes: Int? = null,
    val Compression: Compression? = null,
    val EnableDictionaryCompression: Boolean? = null,
    val MaxPaddingBytes: Int? = null,
    val PageSizeBytes: Int? = null,
    val WriterVersion: WriterVersion? = null
)

@JsonSerializable
data class Serializer(
    val OrcSerDe: OrcSerDe? = null,
    val ParquetSerDe: ParquetSerDe? = null
)

@JsonSerializable
data class OutputFormatConfiguration(
    val Serializer: Serializer? = null
)

@JsonSerializable
data class SchemaConfiguration(
    val CatalogId: String? = null,
    val DatabaseName: String? = null,
    val Region: Region? = null,
    val RoleARN: ARN? = null,
    val TableName: String? = null,
    val VersionId: String? = null
)

@JsonSerializable
data class DataFormatConversionConfiguration(
    val Enabled: Boolean? = null,
    val InputFormatConfiguration: InputFormatConfiguration? = null,
    val OutputFormatConfiguration: OutputFormatConfiguration? = null,
    val SchemaConfiguration: SchemaConfiguration? = null
)

@JsonSerializable
enum class CompressionFormat {
    UNCOMPRESSED, GZIP, ZIP, Snappy, HADOOP_SNAPPY
}

@JsonSerializable
data class S3DestinationConfiguration(
    val BucketARN: ARN,
    val RoleARN: ARN,
    val BufferingHints: BufferingHints? = null,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val CompressionFormat: CompressionFormat? = null,
    val EncryptionConfiguration: EncryptionConfiguration? = null,
    val ErrorOutputPrefix: String? = null,
    val Prefix: String? = null,
)

@JsonSerializable
data class ExtendedS3DestinationConfiguration(
    val BucketARN: ARN,
    val RoleARN: ARN,
    val BufferingHints: BufferingHints? = null,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val CompressionFormat: CompressionFormat? = null,
    val DataFormatConversionConfiguration: DataFormatConversionConfiguration? = null,
    val EncryptionConfiguration: EncryptionConfiguration? = null,
    val ErrorOutputPrefix: String? = null,
    val Prefix: String? = null,
    val ProcessingConfiguration: ProcessingConfiguration? = null,
    val S3BackupConfiguration: S3BackupConfiguration? = null,
    val S3BackupMode: String? = null
)

@JsonSerializable
data class EndpointConfiguration(
    val Url: Uri,
    val AccessKey: String? = null,
    val Name: String? = null
)

@JsonSerializable
data class HttpEndpointCommonAttribute(
    val AttributeName: String,
    val AttributeValue: String
)

@JsonSerializable
enum class ContentEncoding {
    NONE, GZIP
}

@JsonSerializable
data class RequestConfiguration(
    val CommonAttributes: List<HttpEndpointCommonAttribute>? = null,
    val ContentEncoding: ContentEncoding? = null
)

@JsonSerializable
enum class S3BackupMode {
    FailedDataOnly, AllData
}

@JsonSerializable
data class HttpEndpointDestinationConfiguration(
    val S3Configuration: S3DestinationConfiguration?,
    val BufferingHints: BufferingHints? = null,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val EndpointConfiguration: EndpointConfiguration? = null,
    val ProcessingConfiguration: ProcessingConfiguration? = null,
    val RequestConfiguration: RequestConfiguration? = null,
    val RetryOptions: RetryOptions? = null,
    val RoleARN: ARN? = null,
    val S3BackupMode: S3BackupMode? = null
)

@JsonSerializable
data class KinesisStreamSourceConfiguration(
    val KinesisStreamARN: ARN,
    val RoleARN: ARN
)

@JsonSerializable
data class CopyCommand(
    val DataTableName: String,
    val CopyOptions: String? = null,
    val DataTableColumns: String? = null
)

@JsonSerializable
enum class RedshiftBackupMode {
    Disabled, Enabled
}

@JsonSerializable
data class RedshiftDestinationConfiguration(
    val RoleARN: ARN,
    val ClusterJDBCURL: String,
    val CopyCommand: CopyCommand,
    val Username: String,
    val Password: String,
    val S3Configuration: S3DestinationConfiguration,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val ProcessingConfiguration: ProcessingConfiguration? = null,
    val RetryOptions: RetryOptions? = null,
    val S3BackupConfiguration: S3BackupConfiguration? = null,
    val S3BackupMode: RedshiftBackupMode? = null
)

@JsonSerializable
enum class HECEndpointType {
    Raw, Event
}

@JsonSerializable
enum class SplunkS3BackupMode {
    FailedEventsOnly, AllEvents
}

@JsonSerializable
data class SplunkDestinationConfiguration(
    val HECEndpoint: String,
    val HECEndpointType: HECEndpointType,
    val HECToken: UUID,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val HECAcknowledgmentTimeoutInSeconds: Int? = null,
    val ProcessingConfiguration: ProcessingConfiguration? = null,
    val RetryOptions: RetryOptions? = null,
    val S3BackupMode: SplunkS3BackupMode? = null,
    val S3Configuration: S3DestinationConfiguration? = null
)

@JsonSerializable
data class BufferingHints(
    val IntervalInSeconds: Int? = null,
    val SizeInMBs: Int? = null
)

@JsonSerializable
data class KMSEncryptionConfig(
    val AWSKMSKeyARN: ARN
)

@JsonSerializable
enum class NoEncryptionConfig {
    NoEncryption
}

@JsonSerializable
data class EncryptionConfiguration(
    val KMSEncryptionConfig: KMSEncryptionConfig? = null,
    val NoEncryptionConfig: NoEncryptionConfig? = null
)

@JsonSerializable
data class CloudWatchLoggingOptions(
    val Enabled: Boolean? = null,
    val LogGroupName: String? = null,
    val LogStreamName: String? = null
)

@JsonSerializable
data class RetryOptions(
    val DurationInSeconds: Int? = null
)

@JsonSerializable
data class ProcessingConfiguration(
    val Enabled: Boolean? = null,
    val Processors: List<Processor>? = null
)

@JsonSerializable
data class S3BackupConfiguration(
    val BucketARN: ARN? = null,
    val BufferingHints: BufferingHints? = null,
    val CloudWatchLoggingOptions: CloudWatchLoggingOptions? = null,
    val CompressionFormat: String? = null,
    val EncryptionConfiguration: EncryptionConfiguration? = null,
    val ErrorOutputPrefix: String? = null,
    val Prefix: String? = null,
    val RoleARN: ARN? = null
)

@JsonSerializable
data class RequestResponses(
    val ErrorCode: String? = null,
    val ErrorMessage: String? = null,
    val RecordId: String? = null
)
