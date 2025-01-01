package org.http4k.connect.slack.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.slack.SlackAction
import org.http4k.connect.slack.SlackMoshi
import org.http4k.connect.slack.model.SlackResponse
import org.http4k.core.Response
import kotlin.reflect.KClass

abstract class NonNullSlackAction<R : Any>(private val clazz: KClass<R>) : SlackAction<R> {
    override fun toResult(response: Response) = with(response) {
        val bodyString = bodyString()
        when {
            status.successful -> with(SlackMoshi.asA<SlackResponse>(bodyString)) {
                when {
                    ok -> Success(SlackMoshi.asA(bodyString, clazz))
                    else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString))

                }
            }

            else -> Failure(RemoteFailure(toRequest().method, toRequest().uri, status, bodyString))
        }
    }
}
