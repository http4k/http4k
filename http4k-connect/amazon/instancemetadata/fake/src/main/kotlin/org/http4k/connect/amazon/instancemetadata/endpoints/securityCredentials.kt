package org.http4k.connect.amazon.instancemetadata.endpoints

import org.http4k.connect.amazon.core.model.Ec2ProfileName
import org.http4k.connect.amazon.instancemetadata.InstanceMetadata
import org.http4k.connect.amazon.instancemetadata.InstanceMetadataServiceMoshi
import org.http4k.connect.amazon.instancemetadata.model.Ec2Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Path
import org.http4k.lens.value
import org.http4k.routing.bind
import java.time.Clock
import java.time.ZonedDateTime

private val profileNameLens = Path.value(Ec2ProfileName).of("profile_name")
private val credentialsLens = InstanceMetadataServiceMoshi.autoBody<Ec2Credentials>().toLens()

fun listSecurityCredentials(metadata: InstanceMetadata) = "/latest/meta-data/iam/security-credentials" bind GET to {
    Response(OK).body(metadata.profiles.joinToString("\n"))
}

fun getSecurityCredentials(metadata: InstanceMetadata, clock: Clock) =
    "/latest/meta-data/iam/security-credentials/$profileNameLens" bind GET to { request ->
        metadata.getCredentials(profileNameLens(request), ZonedDateTime.now(clock))
            ?.let { Response(OK).with(credentialsLens of it) }
            ?: Response(NOT_FOUND)
    }
