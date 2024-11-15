import dev.forkhandles.result4k.valueOrNull
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.format.AnObject
import org.http4k.format.ChildObject
import org.http4k.format.Moshi
import org.http4k.format.autoDynamoLens

data class AnObject(
    val str: String,
    val num: Int,
    val bool: Boolean,
    val uri: Uri,
    val list: List<ChildObject>,
    val map: ChildObject,
    val nullable: String?
)

// this is our key - note that the name is the same as a field in the "row" object
private val keyAttr = Attribute.string().required("str")

fun main() {
    val http: HttpHandler = JavaHttpClient()

    // create a client
    val client = DynamoDb.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val table = TableName.of("myTable")

    // this is an auto-mapping lens
    val lens = Moshi.autoDynamoLens<AnObject>()

    // an example object to read/write
    val input =
        AnObject("foobar", 123, false, Uri.of("http"), listOf(ChildObject("asd")), ChildObject("34534"), null)

    // we can use the lens to write the item directly to the table
    client.putItem(table, Item = Item().with(lens of input))

    // lookup an item from the database by key
    val item = client.getItem(table, Key = Item(keyAttr of input.str)).valueOrNull()!!.item!!

    // read the object back again!
    val extracted: AnObject = lens(item)

    println(extracted)
}

