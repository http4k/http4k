import org.http4k.aws.AwsCredentials
import org.http4k.client.JavaHttpClient
import org.http4k.config.Environment
import org.http4k.connect.amazon.Environment
import org.http4k.connect.amazon.Profile
import org.http4k.connect.amazon.RegionProvider
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.instancemetadata.Ec2InstanceProfile
import org.http4k.connect.amazon.instancemetadata.FakeInstanceMetadataService
import org.http4k.connect.amazon.sns.FakeSNS
import org.http4k.connect.amazon.sns.Http
import org.http4k.connect.amazon.sns.SNS
import org.http4k.connect.amazon.sns.listTopics
import org.http4k.core.HttpHandler

private const val USE_REAL_CLIENT = false
private val fakeAwsCredentials = AwsCredentials("fake", "fake")

fun main() {
    // we can connect to the real service or the fake (drop in replacement)
    val imdsHttp: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeInstanceMetadataService()
    val snsHttp: HttpHandler = if (USE_REAL_CLIENT) JavaHttpClient() else FakeSNS()

    /*
     * Build a RegionProvider chain with the following steps:
     * 1. Try to get region from AWS_REGION environment variable
     * 2. Try to get region from profile credentials file
     * 3. Try to get region from EC2 Instance Metadata Service
     */
    val regionProviderChain = RegionProvider.Environment(Environment.ENV) orElse
        RegionProvider.Profile(Environment.ENV) orElse
        RegionProvider.Ec2InstanceProfile(imdsHttp)

    // Invoking the chain will return a region if one was found
    val optionalRegion: Region? = regionProviderChain()
    println(optionalRegion)

    // orElseThrow will return a region or throw an exception if one was not found
    val region: Region = regionProviderChain.orElseThrow()
    println(region)

    // create and use an Amazon client with the resolved region
    val sns = SNS.Http(region, { fakeAwsCredentials }, snsHttp)
    val topics = sns.listTopics()
    println(topics)
}
