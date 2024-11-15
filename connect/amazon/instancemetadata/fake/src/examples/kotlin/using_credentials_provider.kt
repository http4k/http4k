import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.instancemetadata.Ec2InstanceProfile
import org.http4k.connect.amazon.sns.Http
import org.http4k.connect.amazon.sns.SNS
import org.http4k.connect.amazon.sns.listTopics

fun main() {
    // build a credentials provider that will attempt to load AWS credentials from the EC2's instance profile
    val credentialsProvider = CredentialsProvider.Ec2InstanceProfile()

    // build a client that will authorize requests with the instance profile credentials
    val sns = SNS.Http(Region.US_EAST_1, credentialsProvider)

    // send a request
    val topics = sns.listTopics()
    println(topics)
}
