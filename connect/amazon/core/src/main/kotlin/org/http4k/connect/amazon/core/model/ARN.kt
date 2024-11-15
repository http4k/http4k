package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.Value
import dev.forkhandles.values.and
import dev.forkhandles.values.minLength

class ARN private constructor(value: String) : StringValue(value) {
    private val parts = value.split(":", "/")
    val partition = parts[1]
    val awsService = AwsService.of(parts[2])
    val region by lazy { Region.of(parts[3]) }
    val account by lazy { AwsAccount.of(parts[4]) }

    private val resource = parts.drop(5)

    val resourceType by lazy {
        if (resource.size > 1) resource[0]
        else error("No resource type found in $value")
    }

    fun <T : ResourceId> resourceId(fn: (String) -> T) =
        when {
            resource.size > 1 -> fn(resource.drop(1).joinToString("/"))
            else -> fn(resource[0])
        }

    companion object : StringValueFactory<ARN>(::ARN, 1.minLength.and { it.startsWith("arn:") }) {
        fun of(
            awsService: AwsService,
            region: Region,
            account: AwsAccount,
            resourceId: ResourceId,
            partition: String = "aws"
        ) = of("arn:$partition:$awsService:$region:$account:$resourceId")

        fun of(
            awsService: AwsService,
            region: Region,
            account: AwsAccount,
            resourcePath: String,
            partition: String = "aws"
        ) = of("arn:$partition:$awsService:$region:$account:$resourcePath")

        fun of(
            awsService: AwsService,
            region: Region,
            account: AwsAccount,
            resourceType: String,
            resourceId: ResourceId,
            partition: String = "aws"
        ) = of("arn:$partition:$awsService:$region:$account:$resourceType:$resourceId")
    }
}

fun StringValue.toARN() = ARN.of(value)

fun <T : Value<String>> StringValueFactory<T>.of(arn: ARN) = of(arn.value)
