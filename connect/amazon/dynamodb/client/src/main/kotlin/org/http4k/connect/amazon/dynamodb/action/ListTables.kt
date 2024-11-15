package org.http4k.connect.amazon.dynamodb.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.dynamodb.DynamoDbAction
import org.http4k.connect.amazon.dynamodb.DynamoDbMoshi
import org.http4k.connect.amazon.dynamodb.model.TableName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class ListTables(val ExclusiveStartTableName: TableName? = null, val Limit: Int? = null) :
    DynamoDbAction<TableList>(TableList::class, DynamoDbMoshi),
    PagedAction<TableName, TableName, TableList, ListTables> {
    override fun next(token: TableName) = copy(ExclusiveStartTableName = token)
}

@JsonSerializable
data class TableList(
    val TableNames: List<TableName>,
    val LastEvaluatedTableName: TableName? = null
) : Paged<TableName, TableName> {
    override fun token() = LastEvaluatedTableName
    override val items = TableNames
}
