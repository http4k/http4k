import org.http4k.connect.amazon.CredentialsChain
import org.http4k.connect.amazon.iamidentitycenter.SSO
import org.http4k.connect.amazon.iamidentitycenter.SSOLogin

// example of using SSO credentials provider
fun main() {
    val providerLoginEnabled = CredentialsChain.SSO()
    val credentials1 = providerLoginEnabled()
    println(credentials1)

    val providerLoginDisabled = CredentialsChain.SSO(login = SSOLogin.disabled)
    val credentials2 = providerLoginDisabled()
    println(credentials2)
}
