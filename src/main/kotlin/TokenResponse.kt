import org.http4k.format.JsonLibAutoMarshallingJson

data class TokenResponse(
    val access_token: String,
    val expiresIn: Int,
    val token_type: String) {

    companion object {
        fun <NODE : Any> lens(json: JsonLibAutoMarshallingJson<NODE>) = json.body("token response").map(
            { json.asA(it, TokenResponse::class) },
            json::asJsonObject
        ).toLens()
    }
}