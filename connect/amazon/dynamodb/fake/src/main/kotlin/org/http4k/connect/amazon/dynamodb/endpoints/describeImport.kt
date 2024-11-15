package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AmazonJsonFake
import org.http4k.connect.amazon.dynamodb.action.DescribeImport
import org.http4k.connect.amazon.dynamodb.action.ImportTableResponse
import org.http4k.connect.amazon.dynamodb.model.ImportTableDescription

fun AmazonJsonFake.describeImport(tableImports: List<ImportTableDescription>) =
    route<DescribeImport> { describeImport ->
        tableImports
            .firstOrNull { it.ImportArn == describeImport.ImportArn }
            ?.let { ImportTableResponse(it) }
            ?: TODO()
    }
