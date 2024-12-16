package org.http4k.connect.amazon.iamidentitycenter.model

import io.matthewnelson.encoding.base16.Base16
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.amazon.core.model.Region
import org.http4k.core.Uri
import java.nio.file.Path
import java.security.MessageDigest

data class SSOProfile(
    val accountId: AwsAccount,
    val roleName: RoleName,
    val region: Region,
    val startUri: Uri,
    val ssoSession: SSOSession? = null
) {
    companion object {
        fun loadProfiles(path: Path) = loadConfigFile(path)
            .profileSections()
            .mapNotNull { (n, s) -> s.toSSOProfile()?.let { n to it } }
            .toMap()
    }
}

fun Map<String, String>.toSSOProfile(): SSOProfile? {
    val ssoAccountId = this["sso_account_id"] ?: return null
    val ssoRoleName = this["sso_role_name"] ?: return null
    val ssoRegion = this["sso_region"] ?: return null
    val startUri = this["sso_start_url"] ?: return null
    val ssoSession = this["sso_session"]
    return SSOProfile(
        accountId = AwsAccount.of(ssoAccountId),
        roleName = RoleName.of(ssoRoleName),
        region = Region.of(ssoRegion),
        startUri = Uri.of(startUri),
        ssoSession = ssoSession?.let { SSOSession.of(it) }
    )
}

fun SSOProfile.cachedTokenPath(dir: Path): Path {
    val input = ssoSession?.toString() ?: startUri.toString()

    return dir.resolve("${sha1hex(input)}.json")
}

private fun sha1hex(input: String): String = MessageDigest.getInstance("SHA-1").apply {
    update(input.toByteArray(Charsets.UTF_8))
}.digest().encodeToString(Base16 { encodeToLowercase = true })

fun SSOProfile.cachedRegistrationPath(dir: Path): Path {
    val region = region.value
    val sessionName = ssoSession?.value
    val startUrl = startUri.toString()
    val input =
        "{\"region\": \"$region\", \"scopes\": null, \"session_name\": \"$sessionName\", \"startUrl\": \"$startUrl\", \"tool\": \"botocore\"}"

    return dir.resolve("${sha1hex(input)}.json")
}
