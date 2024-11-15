package org.http4k.connect.amazon.firehose.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.DeliveryStreamType
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.firehose.FirehoseAction
import org.http4k.connect.amazon.model.DeliveryStreamEncryptionConfigurationInput
import org.http4k.connect.amazon.model.DeliveryStreamName
import org.http4k.connect.amazon.model.ElasticsearchDestinationConfiguration
import org.http4k.connect.amazon.model.ExtendedS3DestinationConfiguration
import org.http4k.connect.amazon.model.HttpEndpointDestinationConfiguration
import org.http4k.connect.amazon.model.KinesisStreamSourceConfiguration
import org.http4k.connect.amazon.model.RedshiftDestinationConfiguration
import org.http4k.connect.amazon.model.S3DestinationConfiguration
import org.http4k.connect.amazon.model.SplunkDestinationConfiguration
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
@ExposedCopyVisibility
data class CreateDeliveryStream internal constructor(
    val DeliveryStreamName: DeliveryStreamName,
    val DeliveryStreamType: DeliveryStreamType,
    val DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
    val ElasticsearchDestinationConfiguration: ElasticsearchDestinationConfiguration? = null,
    val ExtendedS3DestinationConfiguration: ExtendedS3DestinationConfiguration? = null,
    val HttpEndpointDestinationConfiguration: HttpEndpointDestinationConfiguration? = null,
    val KinesisStreamSourceConfiguration: KinesisStreamSourceConfiguration? = null,
    val RedshiftDestinationConfiguration: RedshiftDestinationConfiguration? = null,
    val S3DestinationConfiguration: S3DestinationConfiguration? = null,
    val SplunkDestinationConfiguration: SplunkDestinationConfiguration? = null,
    val Tags: List<Tag>? = null
) : FirehoseAction<CreatedDeliveryStream>(CreatedDeliveryStream::class) {

    constructor(
        ElasticsearchDestinationConfiguration: ElasticsearchDestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        ElasticsearchDestinationConfiguration = ElasticsearchDestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        ExtendedS3DestinationConfiguration: ExtendedS3DestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        ExtendedS3DestinationConfiguration = ExtendedS3DestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        HttpEndpointDestinationConfiguration: HttpEndpointDestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        HttpEndpointDestinationConfiguration = HttpEndpointDestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        KinesisStreamSourceConfiguration: KinesisStreamSourceConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        KinesisStreamSourceConfiguration = KinesisStreamSourceConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        RedshiftDestinationConfiguration: RedshiftDestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        RedshiftDestinationConfiguration = RedshiftDestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        S3DestinationConfiguration: S3DestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        S3DestinationConfiguration = S3DestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )

    constructor(
        SplunkDestinationConfiguration: SplunkDestinationConfiguration,
        DeliveryStreamName: DeliveryStreamName,
        DeliveryStreamType: DeliveryStreamType,
        DeliveryStreamEncryptionConfigurationInput: DeliveryStreamEncryptionConfigurationInput? = null,
        Tags: List<Tag>? = null
    ) : this(
        DeliveryStreamName,
        DeliveryStreamType,
        SplunkDestinationConfiguration = SplunkDestinationConfiguration,
        DeliveryStreamEncryptionConfigurationInput = DeliveryStreamEncryptionConfigurationInput,
        Tags = Tags
    )
}

@JsonSerializable
data class CreatedDeliveryStream(val DeliveryStreamARN: ARN)
