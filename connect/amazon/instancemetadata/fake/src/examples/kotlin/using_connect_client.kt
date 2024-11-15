import org.http4k.client.JavaHttpClient
import org.http4k.connect.amazon.instancemetadata.FakeInstanceMetadataService
import org.http4k.connect.amazon.instancemetadata.Http
import org.http4k.connect.amazon.instancemetadata.InstanceMetadataService
import org.http4k.connect.amazon.instancemetadata.getInstanceIdentityDocument
import org.http4k.connect.amazon.instancemetadata.getLocalIpv4
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

private const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeInstanceMetadataService()

    // create a client
    val client = InstanceMetadataService.Http(http.debug())

    // get local ip address
    val localIp = client.getLocalIpv4()
    println(localIp)

    // get identity document
    val identityDocument = client.getInstanceIdentityDocument()
    println(identityDocument)
}
