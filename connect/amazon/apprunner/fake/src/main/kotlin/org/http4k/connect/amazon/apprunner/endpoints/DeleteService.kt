package org.http4k.connect.amazon.apprunner.endpoints

import ServiceIdAndName
import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.apprunner.action.DeleteService
import org.http4k.connect.amazon.apprunner.action.Service
import org.http4k.connect.amazon.apprunner.model.AppRunnerService
import org.http4k.connect.storage.Storage
import java.util.UUID

fun AwsJsonFake.deleteService(records: Storage<Service>) = route<DeleteService> {
    val idAndName = it.ServiceArn.resourceId(ServiceIdAndName::of)
    records[idAndName.value]
        ?.let {
            records.remove(it.ServiceArn.partition)
            AppRunnerService(UUID(0, 0), it)
        }
}
