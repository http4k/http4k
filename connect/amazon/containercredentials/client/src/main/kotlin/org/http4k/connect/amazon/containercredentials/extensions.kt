package org.http4k.connect.amazon.containercredentials

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.config.EnvironmentKey
import org.http4k.core.Uri
import org.http4k.lens.uri
import org.http4k.lens.value

val AWS_CONTAINER_CREDENTIALS_RELATIVE_URI = EnvironmentKey.uri()
    .map({
        when {
            it.toString().startsWith("http://") -> it
            else -> STANDARD_CC_BASE_URL.path(it.path)
        }
    }, { Uri.of(it.toString().removePrefix(STANDARD_CC_BASE_URL.toString())) })
    .required("AWS_CONTAINER_CREDENTIALS_RELATIVE_URI")

val AWS_CONTAINER_CREDENTIALS_FULL_URI = EnvironmentKey.uri().defaulted(
    "AWS_CONTAINER_CREDENTIALS_FULL_URI",
    AWS_CONTAINER_CREDENTIALS_RELATIVE_URI
)

private val STANDARD_CC_BASE_URL = Uri.of("http://169.254.170.2")

val AWS_CONTAINER_AUTHORIZATION_TOKEN =
    EnvironmentKey.value(ContainerCredentialsAuthToken).optional("AWS_CONTAINER_AUTHORIZATION_TOKEN")

class ContainerCredentialsAuthToken(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ContainerCredentialsAuthToken>(::ContainerCredentialsAuthToken)
}
