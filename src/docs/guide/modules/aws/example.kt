package guide.modules.aws

import org.http4k.aws.AwsCredentialScope
import org.http4k.aws.AwsCredentials
import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.filter.ClientFilters
import java.util.*

fun main(args: Array<String>) {

    val region = "US-East1"
    val service = "S3"
    val accessKey = "myGreatAwsAccessKey"
    val secretKey = "myGreatAwsSecretKey"

    val client = ClientFilters.AwsAuth(
        AwsCredentialScope(region, service),
        AwsCredentials(accessKey, secretKey))
        .then(ApacheClient())

    // create a bucket
    val bucketName = UUID.randomUUID().toString()
    val bucketUri = Uri.of("https://$bucketName.s3.amazonaws.com/")
    println(client(Request(PUT, bucketUri)))

    // get list of buckets with the new bucket in it
    println(client(Request(GET, Uri.of("https://s3.amazonaws.com/"))).bodyString())

    // create a key into the bucket
    val key = UUID.randomUUID().toString()

    val keyUri = Uri.of("https://$bucketName.s3.amazonaws.com/$key")
    println(Request(PUT, keyUri).body("some amazing content that I want stored on S3"))

    // get the keys in the bucket
    println(Request(GET, bucketUri))

    // get the contents of the key in the bucket
    println(Request(GET, keyUri))

    // delete the key in the bucket
    println(Request(GET, keyUri))

    // delete the bucket
    println(Request(GET, bucketUri))
}