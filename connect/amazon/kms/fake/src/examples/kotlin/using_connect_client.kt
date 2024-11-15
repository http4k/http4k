import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.valueOrNull
import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.connect.RemoteFailure
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.kms.FakeKMS
import org.http4k.connect.amazon.kms.Http
import org.http4k.connect.amazon.kms.KMS
import org.http4k.connect.amazon.kms.action.Decrypted
import org.http4k.connect.amazon.kms.action.Encrypted
import org.http4k.connect.amazon.kms.action.KeyCreated
import org.http4k.connect.amazon.kms.createKey
import org.http4k.connect.amazon.kms.decrypt
import org.http4k.connect.amazon.kms.encrypt
import org.http4k.connect.amazon.kms.model.CustomerMasterKeySpec.ECC_NIST_P384
import org.http4k.connect.amazon.kms.model.KeyUsage.ENCRYPT_DECRYPT
import org.http4k.connect.model.Base64Blob
import org.http4k.core.HttpHandler
import org.http4k.filter.debug

const val USE_REAL_CLIENT = false

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val http: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeKMS()

    // create a client
    val client = KMS.Http(Region.of("us-east-1"), { AwsCredentials("accessKeyId", "secretKey") }, http.debug())

    // all operations return a Result monad of the API type
    val createdKeyResult: Result<KeyCreated, RemoteFailure> = client.createKey(ECC_NIST_P384, ENCRYPT_DECRYPT)
    val key: KeyCreated = createdKeyResult.valueOrNull()!!
    println(key)

    // we can encrypt some text...
    val encrypted: Encrypted =
        client.encrypt(KeyId = key.KeyMetadata.KeyId, Base64Blob.encode("hello")).valueOrNull()!!
    println(encrypted.CiphertextBlob.decoded())

    // and decrypt it again!
    val decrypted: Decrypted = client.decrypt(KeyId = key.KeyMetadata.KeyId, encrypted.CiphertextBlob).valueOrNull()!!
    println(decrypted.Plaintext.decoded())
}
