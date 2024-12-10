package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi.convert
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.DeleteItem
import org.http4k.connect.amazon.dynamodb.action.ModifiedItem
import org.http4k.connect.amazon.dynamodb.action.ModifiedItems
import org.http4k.connect.amazon.dynamodb.action.PutItem
import org.http4k.connect.amazon.dynamodb.action.TransactWriteItems
import org.http4k.connect.amazon.dynamodb.action.UpdateItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.UpdateOk
import org.http4k.connect.storage.Storage

fun AwsJsonFake.transactWriteItems(tables: Storage<DynamoTable>) = route<TransactWriteItems> {
    synchronized(tables) {
        val transactionItems = it.toTransactionItems()
        when {
            transactionItems.size != it.TransactItems.size -> JsonError("in tx", "some transactions bad")
            else -> {
                val attempts = transactionItems.attemptUsing(tables)
                when {
                    attempts.isNotEmpty() -> JsonError("in tx", attempts.joinToString(",") { it.toString() })
                    else -> {
                        transactionItems.applyTo(tables)
                        ModifiedItems()
                    }
                }
            }
        }
    }
}

private fun List<DynamoDbAction<ModifiedItem>>.applyTo(tables: Storage<DynamoTable>) {
    forEach {
        when (it) {
            is DeleteItem -> tables.runUpdate(it.TableName, it, tryModifyDelete)
            is PutItem -> tables.runUpdate(it.TableName, it, tryModifyPut)
            is UpdateItem -> tables.runUpdate(it.TableName, it, tryModifyUpdate)
            else -> error("bug $it")
        }
    }
}

private fun List<DynamoDbAction<ModifiedItem>>.attemptUsing(tables: Storage<DynamoTable>) = mapNotNull {
    val result = when (it) {
        is DeleteItem -> tryModifyDelete(it, tables[it.TableName.value]!!)
        is PutItem -> tryModifyPut(it, tables[it.TableName.value]!!)
        is UpdateItem -> tryModifyUpdate(it, tables[it.TableName.value]!!)
        else -> error("bug $it")
    }
    when (result) {
        is UpdateOk -> null
        else -> it
    }
}

private fun TransactWriteItems.toTransactionItems() =
    TransactItems.mapNotNull { write ->
        when {
            write.Delete != null -> convert<Map<String, Any?>, DeleteItem>(write.Delete!!)
            write.Put != null -> convert<Map<String, Any?>, PutItem>(write.Put!!)
            write.Update != null -> convert<Map<String, Any?>, UpdateItem>(write.Update!!)
            else -> null
        }
    }
