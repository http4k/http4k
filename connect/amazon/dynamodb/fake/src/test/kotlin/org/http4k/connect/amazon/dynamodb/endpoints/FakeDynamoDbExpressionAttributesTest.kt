package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource

class FakeDynamoDbExpressionAttributesTest : DynamoDbExpressionAttributesContract(), DynamoDbSource by FakeDynamoDbSource()
