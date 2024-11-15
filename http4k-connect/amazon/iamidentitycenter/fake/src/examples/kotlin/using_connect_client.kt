import org.http4k.connect.amazon.CredentialsProvider
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.iamidentitycenter.SSO
import org.http4k.connect.amazon.iamidentitycenter.model.RoleName
import org.http4k.connect.amazon.iamidentitycenter.model.SSOProfile
import org.http4k.core.Uri

// example of using SSO credentials provider
fun main() {
    val provider = CredentialsProvider.SSO(
        SSOProfile(
            AwsAccount.of("01234567890"),
            RoleName.of("hello"),
            Region.US_EAST_1,
            Uri.of("http://foobar"),
        )
    )

    val credentials = provider()
    println(credentials)
}
