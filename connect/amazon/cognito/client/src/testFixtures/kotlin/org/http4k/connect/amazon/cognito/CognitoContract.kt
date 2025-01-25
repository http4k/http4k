package org.http4k.connect.amazon.cognito

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.amazon.AwsContract
import org.http4k.connect.amazon.cognito.action.Scope
import org.http4k.connect.amazon.cognito.model.AccessToken
import org.http4k.connect.amazon.cognito.model.AttributeType
import org.http4k.connect.amazon.cognito.model.AuthFlow.USER_PASSWORD_AUTH
import org.http4k.connect.amazon.cognito.model.ChallengeName.NEW_PASSWORD_REQUIRED
import org.http4k.connect.amazon.cognito.model.ClientName
import org.http4k.connect.amazon.cognito.model.CloudFrontDomain
import org.http4k.connect.amazon.cognito.model.OAuthFlow.client_credentials
import org.http4k.connect.amazon.cognito.model.PoolName
import org.http4k.connect.amazon.cognito.model.UserCode
import org.http4k.connect.amazon.cognito.model.UserPoolId
import org.http4k.connect.amazon.core.model.Password
import org.http4k.connect.amazon.core.model.Username
import org.http4k.connect.successValue
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ClientFilters.BasicAuth
import org.http4k.filter.ClientFilters.Cookies
import org.http4k.filter.ClientFilters.FollowRedirects
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.cookie.BasicCookieStorage
import org.http4k.hamkrest.hasBody
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenResponse
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.client.OAuthClientCredentials
import org.http4k.security.oauth.format.OAuthMoshi.autoBody
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID.randomUUID


interface CognitoContract : AwsContract {
    private val cognito
        get() =
            Cognito.Http(aws.region, { aws.credentials }, http)

    @Test
    @Disabled
    fun `delete pools`() {
        cognito.listUserPools(60).successValue().UserPools.forEach {
            cognito.deleteUserPool(it.Id)
        }
    }

    @Test
    fun `can load well known keys`() {
        withCognitoPool { id ->
            assertThat(cognito.getJwks(id).successValue().keys.size, equalTo(2))
        }
    }

    @Test
    fun `user pool domain lifecycle`() {
        withCognitoPool { id ->
            cognito.createResourceServer(id)
            val domain = CloudFrontDomain.of(randomUUID().toString())
            cognito.createUserPoolDomain(id, domain).successValue()

            try {
                createUserPoolClient(id)
            } finally {
                cognito.deleteUserPoolDomain(id, domain)
            }
        }
    }

    @Test
    fun `can get access token using client credentials grant`() {
        withCognitoPool { id ->
            val poolClient = createUserPoolClient(id)

            val clientCredentials = Credentials(poolClient.ClientId.value, poolClient.ClientSecret!!.value)

            val client = ClientFilters.OAuthClientCredentials(clientCredentials, listOf("scope/Name"))
                .then(BasicAuth(clientCredentials))
                .then(http)

            val accessToken = client(Request(POST, "/oauth2/token").form("client_id", id.value)).assertAccessTokenIsOk()

            http.verifyJwtSignedCorrectly(id, accessToken.value)
        }
    }

    @Test
    fun `can get access token using auth code grant`() {
        withCognitoPool { id ->
            val poolClient = createUserPoolClient(id)

            val protectedPath = "/getit"

            val cognito = SetBaseUriFrom(Uri.of("https://cognito"))
                .then(http)

            val app = App(
                cognito,
                protectedPath,
                Credentials(poolClient.ClientId.value, poolClient.ClientSecret!!.value)
            )

            var lastUri: Uri = Uri.of("")
            val storage = BasicCookieStorage()
            val browser = FollowRedirects()
                .then(Filter { next ->
                    {
                        lastUri = it.uri
                        next(it)
                    }
                })
                .then(Cookies(storage = storage))
                .then { r ->
                    when (r.uri.host) {
                        "app" -> app
                        else -> cognito
                    }(r)
                }

            assertThat(
                browser(Request(GET, "http://app$protectedPath")),
                hasBody(containsSubstring("Enter your email to log into "))
            )

            assertThat(
                browser(
                    Request(POST, lastUri)
                        .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
                        .form("email", "joe@email.com")
                ), hasBody("LOGGEDIN")
            )

            cognito.verifyJwtSignedCorrectly(
                id,
                storage.retrieve().first { it.cookie.name == "oauthAccessToken" }.cookie.value
            )
        }
    }

    @Test
    @Disabled("WIP")
    fun `user pool operations`() {
        withCognitoPool { id ->
            val username = Username.of(randomUUID().toString())
            adminCreateUser(
                username, id, listOf(
                    AttributeType("email", "test@example.com"), AttributeType("email_verified", "true")
                )
            ).successValue()

            assertThat(adminGetUser(username, id).successValue().Username, equalTo(username))

            adminSetUserPassword(username, id, true, Password.of("Password1£$%4")).successValue()

            adminResetUserPassword(username, id, emptyMap()).successValue()

            adminDisableUser(username, id).successValue()

            adminEnableUser(username, id).successValue()

            adminDeleteUser(username, id).successValue()
        }
    }

    @Test
    @Disabled("WIP")
    fun `user auth lifecycle`() {
        withCognitoPool { id ->
            val username = Username.of(randomUUID().toString())

            adminCreateUser(
                username, id, listOf(
                    AttributeType("email", "test@example.com"),
                    AttributeType("email_verified", "true")
                )
            ).successValue()

            val client = createUserPoolClient(id, ClientName.of(username.value), listOf(client_credentials))
                .successValue().UserPoolClient

            val challenge = initiateAuth(
                client.ClientId, USER_PASSWORD_AUTH, mapOf(
                    "USERNAME" to username.value,
                    "PASSWORD" to "foobar"
                )
            ).successValue()

            associateSoftwareToken(AccessToken.of("1234"), challenge.Session).successValue()

            verifySoftwareToken(UserCode.of("123456"), AccessToken.of("1234"), challenge.Session).successValue()

            respondToAuthChallenge(
                client.ClientId, NEW_PASSWORD_REQUIRED, mapOf(
                    NEW_PASSWORD_REQUIRED to "",
                )
            ).successValue()

            deleteUserPoolClient(id, client.ClientId).successValue()

            adminDeleteUser(username, id).successValue()
        }
    }

    private fun <T> withCognitoPool(fn: Cognito.(UserPoolId) -> T) = with(cognito) {
        val id = createUserPool(PoolName.of(randomUUID().toString())).successValue().UserPool.Id!!
        try {
            fn(id)
        } finally {
            deleteUserPool(id).successValue()
        }
    }
}

private fun Cognito.createUserPoolClient(id: UserPoolId) = createUserPoolClient(
    UserPoolId = id,
    ClientName = ClientName.of(randomUUID().toString()),
    AllowedOAuthFlows = listOf(client_credentials),
    AllowedOAuthFlowsUserPoolClient = true,
    AllowedOAuthScopes = listOf("scope/Name"),
    GenerateSecret = true
).successValue().UserPoolClient

private fun Cognito.createResourceServer(id: UserPoolId) {
    createResourceServer(id, "scope", "scope", listOf(Scope("Name", "Description"))).successValue()
}

private fun Response.assertAccessTokenIsOk(): AccessToken {
    val token = autoBody<AccessTokenResponse>().toLens()(this)
    assertThat(bodyString(), status.successful, equalTo(true))
    assertThat(token.token_type, equalTo("Bearer"))
    assertThat(token.expires_in, equalTo(3600))
    return AccessToken.of(token.access_token)
}

private fun HttpHandler.verifyJwtSignedCorrectly(id: UserPoolId, jwt: String) {
    val jwks = JsonWebKeySet(this(Request(GET, "/$id/.well-known/jwks.json")).bodyString()).jsonWebKeys

    val jws = JsonWebSignature().apply {
        key = jwks.last().key
        compactSerialization = jwt
    }
    assertThat(jws.verifySignature(), equalTo(true))
}

private fun App(
    oauth: HttpHandler, protectedPath: String, credentials: Credentials
): RoutingHttpHandler {
    val callbackPath = "/cb"

    val oauthProvider = OAuthProvider(
        OAuthProviderConfig(
            Uri.of("https://cognito"),
            "/oauth2/authorize", "/oauth2/token",
            credentials
        ),
        oauth,
        Uri.of("http://app$callbackPath"),
        listOf(),
        InsecureCookieBasedOAuthPersistence("oauth")
    )

    return routes(
        callbackPath bind GET to oauthProvider.callback,
        protectedPath bind GET to oauthProvider.authFilter.then { Response(OK).body("LOGGEDIN") }
    )
}
