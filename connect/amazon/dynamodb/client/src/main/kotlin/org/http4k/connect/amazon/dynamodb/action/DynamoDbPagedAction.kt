package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.Key
import kotlin.reflect.KClass

abstract class DynamoDbPagedAction<R : Paged<Key, Item>,
    Self : PagedAction<Key, Item, R, Self>>(clazz: KClass<R>) :
    DynamoDbAction<R>(clazz, DynamoDbMoshi),
    PagedAction<Key, Item, R, Self> {
    abstract val Limit: Int?
}
