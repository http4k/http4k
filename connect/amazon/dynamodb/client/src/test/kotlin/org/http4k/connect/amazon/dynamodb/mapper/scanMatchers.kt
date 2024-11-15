package org.http4k.connect.amazon.dynamodb.mapper

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.connect.amazon.dynamodb.action.Scan
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.TokensToNames
import org.http4k.connect.amazon.dynamodb.model.TokensToValues

fun scanHasFilterExpression(expr: String?) =
    has("FilterExpression", { scan: Scan -> scan.FilterExpression }, equalTo(expr))

fun scanHasAttributeNames(names: TokensToNames?) =
    has("ExpressionAttributeNames", { scan: Scan -> scan.ExpressionAttributeNames }, equalTo(names))

fun scanHasAttributeValues(values: TokensToValues?) =
    has("ExpressionAttributeValues", { scan: Scan -> scan.ExpressionAttributeValues }, equalTo(values))

fun scanHasLimit(limit: Int?) = has("Limit", { scan: Scan -> scan.Limit }, equalTo(limit))

fun scanHasExclusiveStartKey(key: Key?) =
    has("ExclusiveStartKey", { scan: Scan -> scan.ExclusiveStartKey }, equalTo(key))

fun scanHasConsistentRead(value: Boolean?) =
    has("ConsistentRead", { scan: Scan -> scan.ConsistentRead }, equalTo(value))
