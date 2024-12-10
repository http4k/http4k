package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.CreateTable
import org.http4k.connect.amazon.dynamodb.action.TableDescriptionResponse
import org.http4k.connect.amazon.dynamodb.model.GlobalSecondaryIndexResponse
import org.http4k.connect.amazon.dynamodb.model.LocalSecondaryIndexResponse
import org.http4k.connect.amazon.dynamodb.model.ProvisionedThroughputResponse
import org.http4k.connect.amazon.dynamodb.model.TableDescription
import org.http4k.connect.amazon.dynamodb.model.TableStatus
import org.http4k.connect.storage.Storage

fun AwsJsonFake.createTable(tables: Storage<DynamoTable>) = route<CreateTable> {
    val tableDescription = TableDescription(
        AttributeDefinitions = it.AttributeDefinitions,
        ItemCount = 0,
        KeySchema = it.KeySchema,
        TableId = it.TableName.value,
        TableName = it.TableName,
        TableSizeBytes = 0,
        TableStatus = TableStatus.ACTIVE,
        GlobalSecondaryIndexes = it.GlobalSecondaryIndexes?.map { index ->
            GlobalSecondaryIndexResponse(
                IndexName = index.IndexName.value,
                KeySchema = index.KeySchema,
                Projection = index.Projection,
                ProvisionedThroughput = index.ProvisionedThroughput?.let { throughput ->
                    ProvisionedThroughputResponse(
                        ReadCapacityUnits = throughput.ReadCapacityUnits,
                        WriteCapacityUnits = throughput.WriteCapacityUnits
                    )
                }
            )
        },
        LocalSecondaryIndexes = it.LocalSecondaryIndexes?.map { index ->
            LocalSecondaryIndexResponse(
                IndexName = index.IndexName.value,
                KeySchema = index.KeySchema,
                Projection = index.Projection,
            )
        }
    )
    tables[it.TableName.value] = DynamoTable(tableDescription, mutableListOf())

    TableDescriptionResponse(tableDescription)
}
