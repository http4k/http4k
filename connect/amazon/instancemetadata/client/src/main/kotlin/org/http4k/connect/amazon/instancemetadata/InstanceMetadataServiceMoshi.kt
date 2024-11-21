package org.http4k.connect.amazon.instancemetadata

import com.squareup.moshi.JsonAdapter
import org.http4k.connect.amazon.instancemetadata.model.HostName
import org.http4k.connect.amazon.instancemetadata.model.ImageId
import org.http4k.connect.amazon.instancemetadata.model.InstanceId
import org.http4k.connect.amazon.instancemetadata.model.InstanceType
import org.http4k.connect.amazon.instancemetadata.model.IpV4Address
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.AwsMoshiBuilder
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.value
import se.ansman.kotshi.KotshiJsonAdapterFactory

object InstanceMetadataServiceMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(InstanceMetadataServiceJsonAdapterFactory)
        .done()
)

fun <T> AutoMappingConfiguration<T>.withInstanceMetadataServiceMappings() = apply {
    value(HostName)
    value(ImageId)
    value(InstanceId)
    value(InstanceType)
    value(IpV4Address)
}

@KotshiJsonAdapterFactory
object InstanceMetadataServiceJsonAdapterFactory :
    JsonAdapter.Factory by KotshiInstanceMetadataServiceJsonAdapterFactory
