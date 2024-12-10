package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.action.DescribeImport
import org.http4k.connect.amazon.dynamodb.action.ImportTableResponse
import org.http4k.connect.amazon.dynamodb.model.ImportTableDescription

fun AwsJsonFake.describeImport(tableImports: List<ImportTableDescription>) =
    route<DescribeImport> { describeImport ->
        tableImports
            .firstOrNull { it.ImportArn == describeImport.ImportArn }
            ?.let { ImportTableResponse(it) }
            ?: TODO()
    }
