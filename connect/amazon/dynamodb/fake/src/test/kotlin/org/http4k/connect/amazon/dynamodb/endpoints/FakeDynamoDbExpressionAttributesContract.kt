package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource

class FakeDynamoDbExpressionAttributesContract : DynamoDbExpressionAttributesContract(), DynamoDbSource by FakeDynamoDbSource()
