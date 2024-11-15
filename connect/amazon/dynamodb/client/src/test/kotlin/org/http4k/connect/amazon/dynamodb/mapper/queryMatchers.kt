package org.http4k.connect.amazon.dynamodb.mapper

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.connect.amazon.dynamodb.action.Query
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.Select
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues

fun queryHasKeyConditionExpression(expr: String?) =
    has("KeyConditionExpression", { query: Query -> query.KeyConditionExpression }, equalTo(expr))

fun queryHasFilterExpression(expr: String?) =
    has("FilterExpression", { query: Query -> query.FilterExpression }, equalTo(expr))

fun queryHasAttributeNames(names: TokensToNames?) =
    has("ExpressionAttributeNames", { query: Query -> query.ExpressionAttributeNames }, equalTo(names))

fun queryHasAttributeValues(values: TokensToValues?) =
    has("ExpressionAttributeValues", { query: Query -> query.ExpressionAttributeValues }, equalTo(values))

fun queryHasLimit(limit: Int?) = has("Limit", { query: Query -> query.Limit }, equalTo(limit))

fun queryHasExclusiveStartKey(key: Key?) =
    has("ExclusiveStartKey", { query: Query -> query.ExclusiveStartKey }, equalTo(key))

fun queryHasConsistentRead(value: Boolean?) =
    has("ConsistentRead", { query: Query -> query.ConsistentRead }, equalTo(value))

fun queryHasScanIndexForward(value: Boolean?) =
    has("ScanIndexForward", { query: Query -> query.ScanIndexForward }, equalTo(value))

fun queryHasSelect(select: Select) = has("Select", { query: Query -> query.Select }, equalTo(select))
