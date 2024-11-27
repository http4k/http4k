package org.http4k.connect.amazon.core.model

import org.http4k.aws.AwsCredentials
import java.nio.file.Path

data class AwsProfile(
    val name: ProfileName,
    val accessKeyId: AccessKeyId?,
    val secretAccessKey: SecretAccessKey?,
    val sessionToken: SessionToken?,
    val roleArn: ARN?,
    val sourceProfileName: ProfileName?,
    val roleSessionName: RoleSessionName?,
    val region: Region?
) {
    fun getCredentials(): AwsCredentials? {
        return AwsCredentials(
            accessKey = accessKeyId?.value ?: return null,
            secretKey = secretAccessKey?.value ?: return null,
            sessionToken = sessionToken?.value
        )
    }

    companion object {
        fun loadProfiles(path: Path) = loadProfiles(path) { map, name ->
            AwsProfile(
                name = name,
                accessKeyId = map["aws_access_key_id"]?.let(AccessKeyId::of),
                secretAccessKey = map["aws_secret_access_key"]?.let(SecretAccessKey::of),
                sessionToken = map["aws_session_token"]?.let(SessionToken::of),
                roleArn = map["role_arn"]?.let(ARN::of),
                sourceProfileName = map["source_profile"]?.let(ProfileName::of),
                roleSessionName = map["role_session_name"]?.let(RoleSessionName::of),
                region = map["region"]?.let(Region::of)
            )
        }
    }
}
