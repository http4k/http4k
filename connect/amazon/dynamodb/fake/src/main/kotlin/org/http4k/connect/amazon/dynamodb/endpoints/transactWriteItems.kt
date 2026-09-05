package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.AwsJsonFake
import org.http4k.connect.amazon.JsonError
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi.convert
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.action.CancellationReason
import org.http4k.connect.amazon.dynamodb.action.DeleteItem
import org.http4k.connect.amazon.dynamodb.action.ModifiedItem
import org.http4k.connect.amazon.dynamodb.action.ModifiedItems
import org.http4k.connect.amazon.dynamodb.action.PutItem
import org.http4k.connect.amazon.dynamodb.action.TransactWriteItems
import org.http4k.connect.amazon.dynamodb.action.TransactionCanceled
import org.http4k.connect.amazon.dynamodb.action.UpdateItem
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.ConditionFailed
import org.http4k.connect.amazon.dynamodb.endpoints.UpdateResult.UpdateOk
import org.http4k.connect.storage.Storage

fun AwsJsonFake.transactWriteItems(tables: Storage<DynamoTable>) = route<TransactWriteItems>(
    responseFn = { conditionCheckAware(it) }
) { req ->
    // each item's ConditionExpression is evaluated in attemptUsing, so the whole transaction is wrapped
    conditionErrorAware {
        synchronized(tables) {
            val transactionItems = req.toTransactionItems()
            when {
                transactionItems.size != req.TransactItems.size -> JsonError("in tx", "some transactions bad")

                else -> {
                    val attempts = transactionItems.attemptUsing(tables)
                    when {
                        attempts.any { it !is UpdateOk } -> attempts.cancelled()

                        else -> {
                            transactionItems.applyTo(tables)
                            ModifiedItems()
                        }
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

private fun List<DynamoDbAction<ModifiedItem>>.attemptUsing(tables: Storage<DynamoTable>) = map {
    when (it) {
        is DeleteItem -> tryModifyDelete(it, tables[it.TableName.value]!!)
        is PutItem -> tryModifyPut(it, tables[it.TableName.value]!!)
        is UpdateItem -> tryModifyUpdate(it, tables[it.TableName.value]!!)
        else -> error("bug $it")
    }
}

private fun List<UpdateResult>.cancelled(): TransactionCanceled {
    val reasons = map {
        when (it) {
            is UpdateOk -> CancellationReason("None")
            is ConditionFailed -> CancellationReason("ConditionalCheckFailed", "The conditional request failed", it.item)
            else -> error("bug $it")
        }
    }
    return TransactionCanceled(
        __type = "com.amazonaws.dynamodb.v20120810#TransactionCanceledException",
        Message = "Transaction cancelled, please refer to cancellation reasons for specific reasons " +
            reasons.joinToString(", ", "[", "]") { it.Code },
        CancellationReasons = reasons
    )
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
