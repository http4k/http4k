package org.http4k.connect.amazon.sts

import org.http4k.base64Encode
import org.http4k.core.Body
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.routing.asPredicate
import org.http4k.routing.bind
import org.http4k.template.PebbleTemplates
import org.http4k.template.viewModel
import java.time.Clock
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.UUID

fun assumeRole(defaultSessionValidity: Duration, clock: Clock) = { r: Request -> r.form("Action") == "AssumeRole" }
    .asPredicate() bind { req: Request ->
    val duration = req.form("DurationSeconds")
        ?.toLong()
        ?.let(Duration::ofSeconds)
        ?: defaultSessionValidity
    Response(Status.OK).with(
        viewModelLens of AssumeRoleResponse(
            req.form("RoleArn")!!,
            req.form("RoleSessionName")!!,
            "accessKeyId",
            "secretAccessKey",
            UUID.randomUUID().toString().base64Encode(),
            ISO_ZONED_DATE_TIME.format(ZonedDateTime.now(clock) + duration)
        )
    )
}

fun assumeRoleWithWebIdentity(defaultSessionValidity: Duration, clock: Clock) =
    { r: Request -> r.form("Action") == "AssumeRoleWithWebIdentity" }
        .asPredicate() bind { req: Request ->
        val duration = req.form("DurationSeconds")
            ?.toLong()
            ?.let(Duration::ofSeconds)
            ?: defaultSessionValidity
        Response(Status.OK).with(
            viewModelLens of AssumeRoleWithWebIdentityResponse(
                req.form("RoleArn")!!,
                req.form("RoleSessionName")!!,
                "accessKeyId",
                "secretAccessKey",
                UUID.randomUUID().toString().base64Encode(),
                ISO_ZONED_DATE_TIME.format(ZonedDateTime.now(clock) + duration)
            )
        )
    }

private val viewModelLens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), ContentType.APPLICATION_XML).toLens()
}
