import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.iamidentitycenter.SSO

// example of using SSO credentials provider
fun main() {
    val provider = CredentialsChain.SSO()

    val credentials = provider()
    println(credentials)
}
