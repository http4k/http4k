package org.http4k.connect.amazon.sts

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.WwwAuthenticate
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.routing.asRouter
import org.http4k.routing.bind
import org.http4k.template.PebbleTemplates
import org.http4k.template.viewModel
import org.http4k.util.Hex
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.Random

/**
 * Only supports assumed roles.  Otherwise, 401
 */
fun getCallerIdentity(clock: Clock, assumedRoles: Storage<AssumedRole>) =
    { request: Request -> request.form("Action") == "GetCallerIdentity" }
        .asRouter() bind fn@{ request: Request ->

        val accessKeyId = request.header("Authorization")
            ?.let { WwwAuthenticate.parseHeader(it) }
            ?.get("Credential")
            ?.split("/")
            ?.firstOrNull()
            ?: return@fn Response(Status.UNAUTHORIZED)

        val assumedRole = request
            .header("X-Amz-Security-Token")
            ?.let { assumedRoles["$accessKeyId/$it"] }
            ?.takeIf { it.expires >= clock.instant() }
            ?: return@fn Response(Status.UNAUTHORIZED)

        val roleName = assumedRole.arn.toString().split("/").last()

        Response(Status.OK).with(
            viewModelLens of GetCallerIdentityResponse(
                userId = "ARO123EXAMPLE123:${assumedRole.sessionName}",
                account = assumedRole.arn.account.value,
                arn = "arn:aws:sts::${assumedRole.arn.account}:assumed-role/$roleName/${assumedRole.sessionName}"
            )
        )
    }

fun assumeRole(defaultSessionValidity: Duration, clock: Clock, random: Random, assumedRoles: Storage<AssumedRole>) = { r: Request ->
    r.form("Action") == "AssumeRole" }.asRouter() bind { req: Request ->

    val duration = req.durationSeconds() ?: defaultSessionValidity

    val assumedRole = AssumedRole(
        arn = ARN.parse(req.form("RoleArn")!!),
        sessionName = req.form("RoleSessionName")!!,
        expires = clock.instant() + duration
    )

    val accessKey = nextToken(random)
    val sessionToken = nextToken(random)

    assumedRoles["$accessKey/$sessionToken"] = assumedRole

    Response(Status.OK).with(
        viewModelLens of AssumeRoleResponse(
            arn = assumedRole.arn.value,
            roleId = assumedRole.sessionName,
            accessKeyId = accessKey,
            secretAccessKey = nextToken(random),
            sessionToken = sessionToken,
            expiration = ISO_ZONED_DATE_TIME.format(ZonedDateTime.ofInstant(assumedRole.expires, clock.zone))
        )
    )
}

fun assumeRoleWithWebIdentity(defaultSessionValidity: Duration, clock: Clock, random: Random, assumedRoles: Storage<AssumedRole>) =
    { r: Request -> r.form("Action") == "AssumeRoleWithWebIdentity" }
        .asRouter() bind { req: Request ->

        val duration = req.durationSeconds() ?: defaultSessionValidity

        val assumedRole = AssumedRole(
            arn = ARN.parse(req.form("RoleArn")!!),
            sessionName = req.form("RoleSessionName")!!,
            expires = clock.instant() + duration
        )

        val accessKey = nextToken(random)
        val sessionToken = nextToken(random)

        assumedRoles["$accessKey/$sessionToken"] = assumedRole

        Response(Status.OK).with(
            viewModelLens of AssumeRoleWithWebIdentityResponse(
                arn = assumedRole.arn.value,
                roleId = assumedRole.sessionName,
                accessKeyId = accessKey,
                secretAccessKey = nextToken(random),
                sessionToken = sessionToken,
                ISO_ZONED_DATE_TIME.format(ZonedDateTime.ofInstant(assumedRole.expires, clock.zone))
            )
        )
    }

private val viewModelLens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), ContentType.APPLICATION_XML).toLens()
}

private fun nextToken(random: Random) = ByteArray(8)
    .also(random::nextBytes)
    .let(Hex::hex)


private fun Request.durationSeconds() = form("DurationSeconds")
    ?.toLong()
    ?.let(Duration::ofSeconds)
