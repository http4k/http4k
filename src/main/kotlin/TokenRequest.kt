import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.WebForm
import org.http4k.lens.webForm

data class TokenRequest(
    val grantType: String,
    val redirectUri: Uri,
    val client: Credentials,
    val scopes: List<String>) {

    companion object {
        private val grantTypeField = FormField.required("grant_type")
        private val redirectUriField = FormField.map(Uri.Companion::of, Uri::toString).required("redirect_uri")
        private val clientIdField = FormField.required("clientId")
        private val clientSecretField = FormField.required("client_secret")
        private val scopeField = FormField.map({ it.split(" ") }, { it.joinToString(" ") }).required("scope")

        private fun from(webForm: WebForm) = TokenRequest(grantTypeField(webForm),
            redirectUriField(webForm),
            Credentials(clientIdField(webForm), clientSecretField(webForm)),
            scopeField(webForm))

        private fun toWebForm(request: TokenRequest) = WebForm().with(
            grantTypeField.of(request.grantType),
            redirectUriField.of(request.redirectUri),
            clientIdField.of(request.client.user),
            clientSecretField.of(request.client.password),
            scopeField.of(request.scopes)
        )

        val lens = Body.webForm(Validator.Strict).map(TokenRequest.Companion::from, ::toWebForm).toLens()
    }
}