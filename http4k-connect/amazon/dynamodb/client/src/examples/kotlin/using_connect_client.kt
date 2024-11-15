import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.valueOrNull
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.dynamodb.DynamoDb
import org.http4k.connect.amazon.dynamodb.Http
import org.http4k.connect.amazon.dynamodb.action.TableDescriptionResponse
import org.http4k.connect.amazon.dynamodb.deleteTable
import org.http4k.connect.amazon.dynamodb.getItem
import org.http4k.connect.amazon.dynamodb.model.Attribute
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.List
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Null
import org.http4k.connect.amazon.dynamodb.model.AttributeValue.Companion.Num
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.connect.amazon.dynamodb.model.TableName
import org.http4k.connect.amazon.dynamodb.model.with
import org.http4k.connect.amazon.dynamodb.putItem
import org.http4k.connect.model.Base64Blob
import org.http4k.core.HttpHandler
import org.http4k.filter.debug
import org.http4k.lens.LensFailure
import org.http4k.lens.asResult

private val attrBool = Attribute.boolean().required("theBool")
private val attrB = Attribute.base64Blob().required("theBase64Blob")
private val attrBS = Attribute.base64Blobs().required("theBase64Blobs")
private val attrN = Attribute.int().required("theNum")
private val attrNS = Attribute.ints().required("theNums")
private val attrL = Attribute.list().required("theList")
private val attrM = Attribute.map().required("theMap")
private val attrS = Attribute.string().required("theString")
private val attrSS = Attribute.strings().required("theStrings")
private val attrNL = Attribute.string().optional("theNull")

fun main() {
    // we can connect to the real service
    val http: HttpHandler = JavaHttpClient()

    // create a client
    val client = DynamoDb.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    val table = TableName.of("myTable")

    // we can bind typed values to the attributes of an item
    client.putItem(
        table,
        Item = Item(
            attrS of "foobar",
            attrBool of true,
            attrB of Base64Blob.encode("foo"),
            attrBS of setOf(Base64Blob.encode("bar")),
            attrN of 123,
            attrNS of setOf(123, 321),
            attrL of listOf(
                List(listOf(AttributeValue.Str("foo"))),
                Num(123),
                Null()
            ),
            attrM of Item().with(attrS of "foo", attrBool of false),
            attrSS of setOf("345", "567"),
            attrNL of null
        )
    )

    // lookup an item from the database
    val item = client.getItem(table, Key = Item(attrS of "hello")).valueOrNull()!!.item!!
    val str: String = attrS(item)
    val boolean: Result<Boolean, LensFailure> = attrBool.asResult()(item)

    // all operations return a Result monad of the API type
    val deleteResult: Result<TableDescriptionResponse, RemoteFailure> = client.deleteTable(table)
    println(deleteResult)
}

