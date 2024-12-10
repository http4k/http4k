package org.http4k.connect.amazon.apprunner.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.apprunner.action.ListServices
import org.http4k.connect.amazon.apprunner.action.Service
import org.http4k.connect.amazon.apprunner.action.ServiceSummary
import org.http4k.connect.amazon.apprunner.action.ServiceSummaryList
import org.http4k.connect.storage.Storage

fun AwsJsonFake.listServices(records: Storage<Service>) = route<ListServices> {
    ServiceSummaryList(
        records.keySet()
            .mapNotNull { records[it] }
            .map {
                ServiceSummary(
                    it.CreatedAt,
                    it.ServiceArn,
                    it.ServiceId,
                    it.ServiceName,
                    it.ServiceUrl,
                    it.Status,
                    it.UpdatedAt
                )
            }
    )
}
