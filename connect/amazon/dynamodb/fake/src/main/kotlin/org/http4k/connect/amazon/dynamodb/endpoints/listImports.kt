package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.dynamodb.action.ListImports
import org.http4k.connect.amazon.dynamodb.action.ListImportsResponse
import org.http4k.connect.amazon.dynamodb.model.ImportSummary
import org.http4k.connect.amazon.dynamodb.model.ImportTableDescription

fun AmazonJsonFake.listImports(tableImports: List<ImportTableDescription>) = route<ListImports> { listImports ->
    ListImportsResponse(
        ImportSummaryList = tableImports
            .filter { it.TableArn == listImports.TableArn }
            .map {
                ImportSummary(ImportArn = it.ImportArn)
            }
    )
}
