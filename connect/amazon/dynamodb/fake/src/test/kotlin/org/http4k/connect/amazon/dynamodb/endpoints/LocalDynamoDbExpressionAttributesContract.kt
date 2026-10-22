package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.LocalDynamoDbSource

class LocalDynamoDbExpressionAttributesContract : DynamoDbExpressionAttributesContract(), DynamoDbSource by LocalDynamoDbSource()
