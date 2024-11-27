import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.FakeDynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.dynamodb.mapper.tableMapper
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.filter.debug
import java.util.UUID

private const val USE_REAL_CLIENT = false

// define our data class
private data class Person(
    val name: String,
    val id: UUID = UUID.randomUUID()
)

private val john = Person("John")
private val jane = Person("Jane")

fun main() {
    // build client (real or fake)
    val http = if (USE_REAL_CLIENT) JavaHttpClient() else FakeDynamoDb()
    val dynamoDb = DynamoDb.Http(Region.CA_CENTRAL_1, { AwsCredentials("id", "secret") }, http.debug())

    // defined table mapper
    val table = dynamoDb.tableMapper<Person, UUID, Unit>(
        tableName = TableName.of("people"),
        hashKeyAttribute = Attribute.uuid().required("id")
    )

    // create table
    table.createTable()

    // save
    table.save(john)
    table.save(jane)

    // get
    val johnAgain = table.get(john.id)

    // scan
    val people = table.primaryIndex().scan().take(10)

    // delete
    table.delete(john)
}
