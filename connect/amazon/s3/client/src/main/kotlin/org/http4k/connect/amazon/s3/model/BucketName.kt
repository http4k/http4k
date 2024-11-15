package org.http4k.connect.amazon.s3.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.AwsService
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.ResourceId

class BucketName private constructor(value: String) : ResourceId(value) {

    fun requiresPathStyleApi() = value.contains('.')

    fun toUri(region: Region, forcePathStyle: Boolean = false) = when {
        forcePathStyle || requiresPathStyleApi() -> AwsService.of("s3").toUri(region).path("/$value")
        else -> AwsService.of("$this.s3").toUri(region)
    }

    companion object : NonBlankStringValueFactory<BucketName>(::BucketName)
}
