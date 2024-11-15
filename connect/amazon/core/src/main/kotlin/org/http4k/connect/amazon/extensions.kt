package org.http4k.connect.amazon

import org.http4k.aws.AwsCredentials
import org.http4k.config.EnvironmentKey
import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AccessKeyId
import org.http4k.connect.amazon.core.model.ProfileName
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.RoleSessionName
import org.http4k.connect.amazon.core.model.SecretAccessKey
import org.http4k.connect.amazon.core.model.SessionToken
import org.http4k.connect.amazon.core.model.WebIdentityToken
import org.http4k.lens.composite
import org.http4k.lens.string
import org.http4k.lens.value
import java.io.File
import kotlin.io.path.Path

val AWS_REGION = EnvironmentKey.value(Region).required("AWS_REGION")
val AWS_REGION_OPTIONAL = EnvironmentKey.value(Region).optional("AWS_REGION")
val AWS_ACCESS_KEY_ID = EnvironmentKey.value(AccessKeyId).required("AWS_ACCESS_KEY_ID")
val AWS_SECRET_ACCESS_KEY = EnvironmentKey.value(SecretAccessKey).required("AWS_SECRET_ACCESS_KEY")
val AWS_ACCESS_KEY_ID_OPTIONAL = EnvironmentKey.value(AccessKeyId).optional("AWS_ACCESS_KEY_ID")
val AWS_SECRET_ACCESS_KEY_OPTIONAL = EnvironmentKey.value(SecretAccessKey).optional("AWS_SECRET_ACCESS_KEY")
val AWS_SESSION_TOKEN = EnvironmentKey.value(SessionToken).optional("AWS_SESSION_TOKEN")
val AWS_ROLE_ARN = EnvironmentKey.value(ARN).required("AWS_ROLE_ARN")
val AWS_PROFILE = EnvironmentKey.value(ProfileName).defaulted("AWS_PROFILE", ProfileName.of("default"))

/**
 * Use when WebIdentityToken is to be stored in file on disk (eg. in K8S)
 */
val AWS_WEB_IDENTITY_TOKEN_FILE =
    EnvironmentKey.map(::File, File::getAbsolutePath).required("AWS_WEB_IDENTITY_TOKEN_FILE")

/**
 * For directly injecting the WebIdentityToken into the environment.
 */
val AWS_WEB_IDENTITY_TOKEN =
    EnvironmentKey.value(WebIdentityToken).defaulted(
        "AWS_WEB_IDENTITY_TOKEN",
        EnvironmentKey.map(::File, File::getAbsolutePath)
            .map(File::readText)
            .map(WebIdentityToken::of)
            .required("AWS_WEB_IDENTITY_TOKEN_FILE"),
        ""
    )

val AWS_ROLE_SESSION_NAME = EnvironmentKey.value(RoleSessionName).optional("AWS_ROLE_SESSION_NAME")

val AWS_CREDENTIALS = EnvironmentKey.composite {
    AwsCredentials(
        AWS_ACCESS_KEY_ID(it).value,
        AWS_SECRET_ACCESS_KEY(it).value,
        AWS_SESSION_TOKEN(it)?.value
    )
}

val AWS_CREDENTIAL_PROFILES_FILE = EnvironmentKey.string()
    .map({ Path(it) }, { it.toString() })
    .defaulted(
        name = "AWS_CREDENTIAL_PROFILES_FILE",
        default = Path(System.getProperty("user.home")).resolve(".aws/credentials")
    )
