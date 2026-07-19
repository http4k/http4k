package org.http4k.connect.amazon.dynamodb.endpoints

import org.http4k.connect.amazon.dynamodb.DynamoDbSource
import org.http4k.connect.amazon.dynamodb.FakeDynamoDbSource

class FakeDynamoDbUpdateItemTest : DynamoDbUpdateItemContract(), DynamoDbSource by FakeDynamoDbSource()

