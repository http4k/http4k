package org.http4k.connect.amazon.firehose

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.DeliveryStreamType.DirectPut
import org.http4k.connect.amazon.DeliveryStreamType.KinesisStreamAsSource
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.firehose.action.CreateDeliveryStream
import org.http4k.connect.amazon.model.BufferingHints
import org.http4k.connect.amazon.model.CloudWatchLoggingOptions
import org.http4k.connect.amazon.model.Compression.SNAPPY
import org.http4k.connect.amazon.model.CompressionFormat.GZIP
import org.http4k.connect.amazon.model.ContentEncoding
import org.http4k.connect.amazon.model.CopyCommand
import org.http4k.connect.amazon.model.DataFormatConversionConfiguration
import org.http4k.connect.amazon.model.DeliveryStreamEncryptionConfigurationInput
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.connect.amazon.model.Deserializer
import org.http4k.connect.amazon.model.ElasticS3BackupMode.AllDocuments
import org.http4k.connect.amazon.model.ElasticsearchDestinationConfiguration
import org.http4k.connect.amazon.model.EncryptionConfiguration
import org.http4k.connect.amazon.model.EndpointConfiguration
import org.http4k.connect.amazon.model.ExtendedS3DestinationConfiguration
import org.http4k.connect.amazon.model.FormatVersion.V0_12
import org.http4k.connect.amazon.model.HECEndpointType.Event
import org.http4k.connect.amazon.model.HiveJsonSerDe
import org.http4k.connect.amazon.model.HttpEndpointCommonAttribute
import org.http4k.connect.amazon.model.HttpEndpointDestinationConfiguration
import org.http4k.connect.amazon.model.IndexRotationPeriod.OneHour
import org.http4k.connect.amazon.model.InputFormatConfiguration
import org.http4k.connect.amazon.model.KMSEncryptionConfig
import org.http4k.connect.amazon.model.KeyType.CUSTOMER_MANAGED_CMK
import org.http4k.connect.amazon.model.KinesisStreamSourceConfiguration
import org.http4k.connect.amazon.model.OpenXJsonSerDe
import org.http4k.connect.amazon.model.OrcSerDe
import org.http4k.connect.amazon.model.OutputFormatConfiguration
import org.http4k.connect.amazon.model.ParameterName.LambdaArn
import org.http4k.connect.amazon.model.ParquetSerDe
import org.http4k.connect.amazon.model.ProcessingConfiguration
import org.http4k.connect.amazon.model.Processor
import org.http4k.connect.amazon.model.ProcessorParameter
import org.http4k.connect.amazon.model.RedshiftBackupMode.Enabled
import org.http4k.connect.amazon.model.RedshiftDestinationConfiguration
import org.http4k.connect.amazon.model.RequestConfiguration
import org.http4k.connect.amazon.model.RetryOptions
import org.http4k.connect.amazon.model.S3BackupConfiguration
import org.http4k.connect.amazon.model.S3BackupMode.AllData
import org.http4k.connect.amazon.model.S3DestinationConfiguration
import org.http4k.connect.amazon.model.SchemaConfiguration
import org.http4k.connect.amazon.model.Serializer
import org.http4k.connect.amazon.model.SplunkDestinationConfiguration
import org.http4k.connect.amazon.model.SplunkS3BackupMode.AllEvents
import org.http4k.connect.amazon.model.VpcConfiguration
import org.http4k.connect.amazon.model.WriterVersion.V2
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

class FirehoseMoshiTest {

    @Test
    fun `S3 destination survives a JSON round trip`() =
        assertRoundTrips(CreateDeliveryStream(s3Destination(), streamName, DirectPut, encryption, tags))

    @Test
    fun `extended S3 destination survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            ExtendedS3DestinationConfiguration(
                BucketARN = arn,
                RoleARN = arn,
                BufferingHints = BufferingHints(60, 5),
                CloudWatchLoggingOptions = loggingOptions,
                CompressionFormat = GZIP,
                DataFormatConversionConfiguration = DataFormatConversionConfiguration(
                    Enabled = true,
                    InputFormatConfiguration = InputFormatConfiguration(
                        Deserializer(
                            HiveJsonSerDe(listOf("yyyy-MM-dd")),
                            OpenXJsonSerDe(true, mapOf("a" to "b"), false)
                        )
                    ),
                    OutputFormatConfiguration = OutputFormatConfiguration(
                        Serializer(
                            OrcSerDe(1, listOf("col"), 2, SNAPPY, 3, true, V0_12, 4, 5, 6),
                            ParquetSerDe(7, SNAPPY, true, 8, 9, V2)
                        )
                    ),
                    SchemaConfiguration = SchemaConfiguration(
                        "catalog", "db", Region.of("ldn-north-1"), arn, "table", "1"
                    )
                ),
                EncryptionConfiguration = encryptionConfiguration,
                ErrorOutputPrefix = "errors/",
                Prefix = "data/",
                ProcessingConfiguration = processingConfiguration,
                S3BackupConfiguration = S3BackupConfiguration(
                    BucketARN = arn,
                    BufferingHints = BufferingHints(10, 1),
                    CloudWatchLoggingOptions = loggingOptions,
                    CompressionFormat = "GZIP",
                    EncryptionConfiguration = encryptionConfiguration,
                    ErrorOutputPrefix = "backup-errors/",
                    Prefix = "backup/",
                    RoleARN = arn
                ),
                S3BackupMode = "Enabled"
            ),
            streamName, DirectPut, encryption, tags
        )
    )

    @Test
    fun `elasticsearch destination survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            ElasticsearchDestinationConfiguration(
                RoleARN = arn,
                IndexName = "index",
                S3Configuration = s3Destination(),
                BufferingHints = BufferingHints(60, 5),
                CloudWatchLoggingOptions = loggingOptions,
                ClusterEndpoint = "https://es.example.com",
                DomainARN = arn,
                IndexRotationPeriod = OneHour,
                ProcessingConfiguration = processingConfiguration,
                RetryOptions = RetryOptions(30),
                S3BackupMode = AllDocuments,
                TypeName = "type",
                VpcConfiguration = VpcConfiguration(arn, listOf("sg-1"), listOf("subnet-1"))
            ),
            streamName, DirectPut, encryption, tags
        )
    )

    @Test
    fun `http endpoint destination survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            HttpEndpointDestinationConfiguration(
                S3Configuration = s3Destination(),
                BufferingHints = BufferingHints(60, 5),
                CloudWatchLoggingOptions = loggingOptions,
                EndpointConfiguration = EndpointConfiguration(
                    Uri.of("https://example.com/firehose"), "access-key", "endpoint"
                ),
                ProcessingConfiguration = processingConfiguration,
                RequestConfiguration = RequestConfiguration(
                    listOf(HttpEndpointCommonAttribute("name", "value")), ContentEncoding.GZIP
                ),
                RetryOptions = RetryOptions(30),
                RoleARN = arn,
                S3BackupMode = AllData
            ),
            streamName, DirectPut, encryption, tags
        )
    )

    @Test
    fun `kinesis stream source survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            KinesisStreamSourceConfiguration(arn, arn),
            streamName, KinesisStreamAsSource, encryption, tags
        )
    )

    @Test
    fun `redshift destination survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            RedshiftDestinationConfiguration(
                RoleARN = arn,
                ClusterJDBCURL = "jdbc:redshift://example.com:5439/db",
                CopyCommand = CopyCommand("table", "FORMAT JSON", "col1,col2"),
                Username = "user",
                Password = "password",
                S3Configuration = s3Destination(),
                CloudWatchLoggingOptions = loggingOptions,
                ProcessingConfiguration = processingConfiguration,
                RetryOptions = RetryOptions(30),
                S3BackupConfiguration = S3BackupConfiguration(BucketARN = arn, RoleARN = arn),
                S3BackupMode = Enabled
            ),
            streamName, DirectPut, encryption, tags
        )
    )

    @Test
    fun `splunk destination survives a JSON round trip`() = assertRoundTrips(
        CreateDeliveryStream(
            SplunkDestinationConfiguration(
                HECEndpoint = "https://splunk.example.com",
                HECEndpointType = Event,
                HECToken = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                CloudWatchLoggingOptions = loggingOptions,
                HECAcknowledgmentTimeoutInSeconds = 180,
                ProcessingConfiguration = processingConfiguration,
                RetryOptions = RetryOptions(30),
                S3BackupMode = AllEvents,
                S3Configuration = s3Destination()
            ),
            streamName, DirectPut, encryption, tags
        )
    )

    private fun assertRoundTrips(action: CreateDeliveryStream) = assertThat(
        FirehoseMoshi.asA<CreateDeliveryStream>(FirehoseMoshi.asFormatString(action)), equalTo(action)
    )

    private val arn = ARN.of("arn:partition:kms:ldn-north-1:001234567890:key:foobar")
    private val streamName = DeliveryStreamName.of("a-delivery-stream")
    private val tags = listOf(Tag("key", "value"))
    private val encryption = DeliveryStreamEncryptionConfigurationInput(CUSTOMER_MANAGED_CMK, arn)
    private val loggingOptions = CloudWatchLoggingOptions(true, "log-group", "log-stream")
    private val encryptionConfiguration = EncryptionConfiguration(KMSEncryptionConfig(arn))
    private val processingConfiguration = ProcessingConfiguration(
        Enabled = true,
        Processors = listOf(Processor(listOf(ProcessorParameter(LambdaArn, "arn:aws:lambda:::function:f"))))
    )

    private fun s3Destination() = S3DestinationConfiguration(
        BucketARN = arn,
        RoleARN = arn,
        BufferingHints = BufferingHints(60, 5),
        CloudWatchLoggingOptions = loggingOptions,
        CompressionFormat = GZIP,
        EncryptionConfiguration = encryptionConfiguration,
        ErrorOutputPrefix = "errors/",
        Prefix = "data/"
    )
}
