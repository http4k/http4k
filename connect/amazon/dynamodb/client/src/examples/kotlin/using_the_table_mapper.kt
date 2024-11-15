import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.dynamodb.mapper.DynamoDbTableMapperSchema
import org.http4k.connect.amazon.dynamodb.mapper.batchDelete
import org.http4k.connect.amazon.dynamodb.mapper.count
import org.http4k.connect.amazon.dynamodb.mapper.get
import org.http4k.connect.amazon.dynamodb.mapper.plusAssign
import org.http4k.connect.amazon.dynamodb.mapper.query
import org.http4k.connect.amazon.dynamodb.mapper.scan
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.IndexName
import org.http4k.connect.amazon.dynamodb.model.TableName
import java.util.UUID

// define our data class
private data class KittyCat(
    val ownerId: UUID,
    val name: String,
    val id: UUID = UUID.randomUUID()
)

// define our key attributes (for primary and secondary indexes)
private val idAttr = Attribute.uuid().required("id")
private val ownerIdAttr = Attribute.uuid().required("ownerId")
private val nameAttr = Attribute.string().required("name")

// define the primary index
private val primaryIndex = DynamoDbTableMapperSchema.Primary<KittyCat, UUID, Unit>(idAttr)

// define an optional secondary index
private val ownerIndex = DynamoDbTableMapperSchema.GlobalSecondary<KittyCat, UUID, UUID>(
    indexName = IndexName.of("owners"),
    hashKeyAttribute = ownerIdAttr,
    sortKeyAttribute = idAttr
)

fun main() {
    val dynamoDb = DynamoDb.Http(System.getenv())

    // define the table mapper and its primary index
    val table = dynamoDb.tableMapper<KittyCat, UUID, Unit>(
        tableName = TableName.of("cats"),
        primarySchema = primaryIndex
    )

    // optionally, create the table and its secondary indexes
    table.createTable(ownerIndex)

    // generate some documents
    val owner1 = UUID.randomUUID()
    val owner2 = UUID.randomUUID()

    val tigger = KittyCat(owner1, "Tigger")
    val smokie = KittyCat(owner2, "Smokie")
    val bandit = KittyCat(owner2, "Bandit")
    val bailey = KittyCat(owner1, "Bailey")
    val shadow = KittyCat(owner2, "Shadow")

    // add the documents to the table
    table += tigger  // ...individually
    table += listOf(smokie, bandit, bailey, shadow) // ...batched

    // get documents
    val cat = table[tigger.id] // individually
    val cats = table[tigger.id, smokie.id]  // batched

    // query documents
    val ownerCats = table.index(ownerIndex).query(owner2).take(100)

    // scan all cats with filter by name
    val bCats = table.primaryIndex().scan {
        filterExpression { nameAttr beginsWith "B" }
    }

    // query cats by owner (key) and name (non-key)
    val aoCats = table.index(ownerIndex).query {
        keyCondition { hashKey eq owner2 }
        filterExpression { (nameAttr contains "a") and (nameAttr contains "o") }
    }

    // count owner1's cats
    val number = table.index(ownerIndex).count {
        keyCondition { hashKey eq owner1 }
    }

    // delete documents
    table.delete(tigger) // ...individually
    table.delete(smokie.id)  // ...by key
    table.batchDelete(bandit.id, bailey.id, shadow.id) // ...batched
}
