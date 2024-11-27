package org.http4k.connect.amazon.dynamodb.mapper

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.isEmpty
import org.http4k.connect.amazon.dynamodb.DynamoTable
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeDefinition
import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.DynamoDataType
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.Key
import org.http4k.connect.amazon.dynamodb.model.KeySchema
import org.http4k.connect.amazon.dynamodb.model.Projection
import org.http4k.connect.amazon.dynamodb.model.ProjectionType
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.compound
import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.successValue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

private val ownerIdAttr = Attribute.uuid().required("ownerId")
private val nameAttr = Attribute.string().required("name")
private val nickNameAttr = Attribute.string().required("nickName")
private val bornAttr = Attribute.localDate().required("born")
private val idAttr = Attribute.uuid().required("id")

private val byOwner = DynamoDbTableMapperSchema.GlobalSecondary<Cat, UUID, String>(
    indexName = IndexName.of("by-owner"),
    hashKeyAttribute = ownerIdAttr,
    sortKeyAttribute = nameAttr
)

private val byDob = DynamoDbTableMapperSchema.GlobalSecondary<CatRef, LocalDate, UUID>(
    indexName = IndexName.of("by-dob"),
    hashKeyAttribute = bornAttr,
    sortKeyAttribute = idAttr,
    projection = Projection(listOf(nameAttr.name), ProjectionType.INCLUDE)
)

class DynamoDbTableMapperTest {

    private val storage: Storage<DynamoTable> = Storage.InMemory()
    private val tableMapper = FakeDynamoDb(storage).client().tableMapper<Cat, UUID, Unit>(
        tableName = TableName.of("cats"),
        hashKeyAttribute = idAttr
    )

    init {
        tableMapper.createTable(byOwner, byDob)
        tableMapper += listOf(toggles, smokie, bandit, kratos, athena)
    }

    private fun table() = storage["cats"]!!

    @Test
    fun `verify cats table`() {
        val tableData = table().table

        assertThat(tableData.TableName, equalTo(TableName.of("cats")))
        assertThat(tableData.KeySchema, equalTo(KeySchema.compound(AttributeName.of("id"))))
        assertThat(
            tableData.AttributeDefinitions?.toSet(),
            equalTo(
                setOf(
                    AttributeDefinition(AttributeName.of("id"), DynamoDataType.S),
                    AttributeDefinition(AttributeName.of("ownerId"), DynamoDataType.S),
                    AttributeDefinition(AttributeName.of("name"), DynamoDataType.S),
                    AttributeDefinition(AttributeName.of("born"), DynamoDataType.S)
                )
            )
        )
        assertThat(tableData.GlobalSecondaryIndexes.orEmpty(), hasSize(equalTo(2)))
        assertThat(tableData.LocalSecondaryIndexes, absent())
    }

    @Test
    fun `scan table`() {
        assertThat(
            tableMapper.primaryIndex().scan().toSet(),
            equalTo(setOf(toggles, smokie, bandit, kratos, athena))
        )
    }

    @Test
    fun `get item`() {
        assertThat(tableMapper[toggles.id], equalTo(toggles))
    }

    @Test
    fun `get missing item`() {
        assertThat(tableMapper[UUID.randomUUID()], absent())
    }

    @Test
    fun `query for index`() {
        assertThat(
            tableMapper.index(byOwner).query(owner2).toList(),
            equalTo(listOf(bandit, smokie))
        )
    }

    @Test
    fun `query for index - reverse order`() {
        assertThat(
            tableMapper.index(byOwner).query(owner2, ScanIndexForward = false).toList(),
            equalTo(listOf(smokie, bandit))
        )
    }

    @Test
    fun `delete item`() {
        tableMapper -= toggles

        assertThat(table().items, hasSize(equalTo(4)))
    }

    @Test
    fun `delete missing item`() {
        tableMapper.delete(UUID.randomUUID())

        assertThat(table().items, hasSize(equalTo(5)))
    }

    @Test
    fun `delete batch`() {
        tableMapper -= listOf(smokie, bandit)

        assertThat(table().items, hasSize(equalTo(3)))
    }

    @Test
    fun `delete batch by ids`() {
        tableMapper.batchDelete(smokie.id, bandit.id)

        assertThat(table().items, hasSize(equalTo(3)))
    }

    @Test
    fun `delete batch by keys`() {
        tableMapper.batchDelete(listOf(smokie.id to null, bandit.id to null))

        assertThat(table().items, hasSize(equalTo(3)))
    }

    @Test
    fun `delete table`() {
        tableMapper.deleteTable().successValue()
        assertThat(storage["cats"], absent())
    }

    @Test
    fun `custom query`() {
        val results = tableMapper.index(byDob).query(
            KeyConditionExpression = "$bornAttr = :val1",
            ExpressionAttributeValues = mapOf(":val1" to bornAttr.asValue(smokie.born))
        ).toList()

        assertThat(results, equalTo(listOf(smokie.ref(), bandit.ref())))
    }

    @Test
    fun `custom query with DSL`() {
        val results = tableMapper.index(byOwner).query {
            keyCondition {
                hashKey eq owner1
            }
            filterExpression {
                val idExpr = idAttr eq kratos.id
                val bornExpr = bornAttr gt toggles.born

                not(idExpr) and bornExpr
            }
        }.toList()

        assertThat(results, equalTo(listOf(athena)))
    }

    @Test
    fun `custom query with filter functions in DSL`() {
        val results = tableMapper.index(byOwner).query {
            keyCondition {
                hashKey eq owner1
            }
            filterExpression {
                nameAttr contains "o"
            }
        }.toList()

        assertThat(results, equalTo(listOf(kratos, toggles)))
    }

    @Test
    fun `custom scan with DSL filter expression`() {
        val results = tableMapper.index(byOwner).scan {
            filterExpression {
                (nameAttr ne nickNameAttr) and (ownerIdAttr eq owner1)
            }
        }.toList()

        assertThat(results, equalTo(listOf(athena)))
    }

    @Test
    fun `query page`() {
        // page 1 of 2
        assertThat(
            tableMapper.index(byOwner).queryPage(owner1, Limit = 2), equalTo(
                DynamoDbPage(
                    items = listOf(athena, kratos),
                    lastEvaluatedKey = Key(
                        idAttr of kratos.id,
                        ownerIdAttr of kratos.ownerId,
                        nameAttr of kratos.name
                    )
                )
            )
        )

        // page 2 of 2
        assertThat(
            tableMapper.index(byOwner).queryPage(
                HashKey = owner1,
                Limit = 2,
                ExclusiveStartKey = Key(
                    idAttr of kratos.id,
                    ownerIdAttr of kratos.ownerId,
                    nameAttr of kratos.name
                )
            ), equalTo(
                DynamoDbPage(
                    items = listOf(toggles),
                    lastEvaluatedKey = null
                )
            )
        )
    }

    @Test
    fun `custom query page`() {
        // page 1 of 2
        assertThat(tableMapper.index(byDob).queryPage(
            KeyConditionExpression = "$bornAttr = :val1",
            ExpressionAttributeValues = mapOf(":val1" to bornAttr.asValue(smokie.born)),
            Limit = 1
        ), equalTo(DynamoDbPage(
            items = listOf(smokie.ref()),
            lastEvaluatedKey = Key(
                idAttr of smokie.id,
                bornAttr of smokie.born
            )
        )))

        // page 2 of 2
        assertThat(tableMapper.index(byDob).queryPage(
            KeyConditionExpression = "$bornAttr = :val1",
            ExpressionAttributeValues = mapOf(":val1" to bornAttr.asValue(smokie.born)),
            Limit = 1,
            ExclusiveStartKey = Key(
                idAttr of smokie.id,
                bornAttr of smokie.born
            )
        ), equalTo(DynamoDbPage(
            items = listOf(bandit.ref()),
            lastEvaluatedKey = null
        )))
    }

    @Test
    fun `custom query page with DSL condition`() {
        // page 1 of 2
        val page1 = tableMapper.index(byDob).queryPage(Limit = 1) {
            keyCondition {
                hashKey eq smokie.born
            }
        }
        assertThat(page1, equalTo(DynamoDbPage(
            items = listOf(smokie.ref()),
            lastEvaluatedKey = Key(
                idAttr of smokie.id,
                bornAttr of smokie.born
            )
        )))

        // page 2 of 2
        val page2 = tableMapper.index(byDob).queryPage(
            Limit = 1,
            ExclusiveStartKey = Key(
                idAttr of smokie.id,
                bornAttr of smokie.born
            )
        ) {
            keyCondition {
                hashKey eq smokie.born
            }
        }
        assertThat(
            page2,
            equalTo(DynamoDbPage(items = listOf(bandit.ref()), lastEvaluatedKey = null))
        )
    }

    @Test
    fun `scan page - secondary index`() {
        // page 1 of 1
        assertThat(
            tableMapper.index(byOwner).scanPage(Limit = 3), equalTo(
                DynamoDbPage(
                    items = listOf(bandit, smokie, athena),
                    lastEvaluatedKey = Key(
                        idAttr of athena.id,
                        ownerIdAttr of athena.ownerId,
                        nameAttr of athena.name
                    )
                )
            )
        )

        // page 2 of 2
        assertThat(
            tableMapper.index(byOwner).scanPage(
                Limit = 3,
                ExclusiveStartKey = Key(
                    idAttr of athena.id,
                    ownerIdAttr of athena.ownerId,
                    nameAttr of athena.name
                )
            ), equalTo(
                DynamoDbPage(
                    items = listOf(kratos, toggles),
                    lastEvaluatedKey = null
                )
            )
        )
    }

    @Test
    fun `scan page - primary index with no sort key`() {
        // page 1 of 1
        assertThat(
            tableMapper.primaryIndex().scanPage(Limit = 3), equalTo(
                DynamoDbPage(
                    items = listOf(smokie, kratos, bandit),
                    lastEvaluatedKey = Key(
                        idAttr of bandit.id
                    )
                )
            )
        )

        // page 2 of 2
        assertThat(
            tableMapper.primaryIndex().scanPage(
                Limit = 3,
                ExclusiveStartKey = Key(
                    idAttr of bandit.id
                )
            ), equalTo(
                DynamoDbPage(
                    items = listOf(toggles, athena),
                    lastEvaluatedKey = null
                )
            )
        )
    }

    @Test
    fun `get empty batch`() {
        val batchGetResult = tableMapper.batchGet(emptyList()).toList()
        assertThat(batchGetResult, isEmpty)
    }

    @Test
    fun `get batch`() {
        val cats = (1..150).map { index ->
            Cat(
                ownerId = UUID.randomUUID(),
                id = UUID.randomUUID(),
                name = "cat$index",
                born = LocalDate.EPOCH
            )
        }

        tableMapper += cats

        val batchGetResult = tableMapper[cats.map { it.id }].toList()
        assertThat(batchGetResult, equalTo(cats))
    }

    @Test
    fun `count (via scan)`() {
        val totalCount = tableMapper.primaryIndex().count()
        assertThat(totalCount, equalTo(5))
    }

    @Test
    fun `count (via scan with filter)`() {
        val totalCount = tableMapper.primaryIndex().count {
            filterExpression {
                bornAttr gt LocalDate.of(2010, 1, 1)
            }
        }
        assertThat(totalCount, equalTo(4))
    }

    @Test
    fun `count (via query)`() {
        val totalCount = tableMapper.index(byOwner).count {
            keyCondition {
                hashKey eq owner1
            }
        }
        assertThat(totalCount, equalTo(3))
    }
}
