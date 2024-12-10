package org.http4k.connect.amazon.apprunner.endpoints

import ServiceIdAndName
import dev.forkhandles.values.ZERO
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.apprunner.action.CreateService
import org.http4k.connect.amazon.apprunner.action.Service
import org.http4k.connect.amazon.apprunner.model.AppRunnerService
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import org.http4k.connect.storage.Storage
import org.http4k.core.Uri
import java.util.UUID

fun AwsJsonFake.createService(records: Storage<Service>) = route<CreateService> {
    val arn = ARN.of("arn:aws:apprunner:us-east-1:000000000001:service/${it.ServiceName.value}/${UUID(0, 0)}")
    val idAndName = arn.resourceId(ServiceIdAndName::of)
    val service = Service(
        arn,
        idAndName.serviceId,
        idAndName.serviceName,
        "RUNNING",
        Timestamp.ZERO, Timestamp.ZERO,
        Uri.of("http://${idAndName}")
    )
    records[idAndName.value] = service
    AppRunnerService(UUID(0, 0), service)
}
