package org.http4k.connect.amazon.instancemetadata

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.amazon.instancemetadata.model.HostName
import org.http4k.connect.amazon.instancemetadata.model.ImageId
import org.http4k.connect.amazon.instancemetadata.model.InstanceId
import org.http4k.connect.amazon.instancemetadata.model.InstanceType
import org.http4k.connect.amazon.instancemetadata.model.IpV4Address
import org.http4k.format.AutoMappingConfiguration
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withAwsCoreMappings
import org.http4k.format.withStandardMappings
import se.ansman.kotshi.KotshiJsonAdapterFactory

object InstanceMetadataServiceMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .add(InstanceMetadataServiceJsonAdapterFactory)
        .asConfigurable()
        .withStandardMappings()
        .withAwsCoreMappings()
        .withInstanceMetadataServiceMappings()
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
