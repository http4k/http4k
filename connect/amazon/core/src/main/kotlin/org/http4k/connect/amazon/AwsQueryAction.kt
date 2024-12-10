package org.http4k.connect.amazon

import com.github.underscore.Xml
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import org.http4k.connect.Action
import org.http4k.connect.RemoteFailure
import org.http4k.connect.asRemoteFailure
import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.body.form
import org.http4k.core.with
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.http4k.lens.Header
import kotlin.reflect.KClass

abstract class AwsQueryAction<Req, Rsp : Any>(
    private val base: Request,
    private val clazz: KClass<Rsp>,
    private val autoMarshalling: AutoMarshallingJson<MoshiNode>,
    private val actionName: String,
    private val version: String,
    private val wrapper: String?
) : Action<Result4k<Rsp, RemoteFailure>> {

    override fun toRequest() =
        (listOf(
            "Action" to actionName,
            "Version" to version
        ) + toQueryParams().toList())
            .fold(base.with(Header.CONTENT_TYPE of ContentType.APPLICATION_FORM_URLENCODED)) { acc, it -> acc.form(it.first, it.second) }

    @Suppress("UNCHECKED_CAST")
    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> {
                val wrapped =
                    transformMemberNodes(Xml.fromXml(bodyString()) as Map<String, Any>)
                val unwrapped = if (wrapper != null) unwrap(wrapped) else wrapped
                Success(autoMarshalling.asA(autoMarshalling.asFormatString(unwrapped), clazz))
            }

            else -> Failure(asRemoteFailure(this))
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun unwrap(fromXml: Any?): Map<String, Any?> {
        val (_, inner) = (fromXml as Map<String, Any>).entries.first()
        return (inner as Map<String, Any>)[wrapper] as Map<String, Any?>
    }

    private fun toQueryParams() = (autoMarshalling.asJsonObject(this) as MoshiObject).toList()
}


@Suppress("UNCHECKED_CAST")
private fun transformMemberNodes(map: Map<String, Any>): Map<String, Any> = map.entries.associate { (key, value) ->
    key to when (value) {
        is Map<*, *> -> {
            val valueMap = value as Map<String, Any>
            // If this map contains a "member" key, replace the entire map with the member contents
            when {
                valueMap.containsKey("member") -> when (val members = valueMap["member"]) {
                    is List<*> -> members.map {
                        if (it is Map<*, *>) transformMemberNodes(it as Map<String, Any>)
                        else it
                    }

                    is Map<*, *> -> listOf(transformMemberNodes(members as Map<String, Any>))
                    else -> emptyList<Any>()
                }
                else -> transformMemberNodes(valueMap)
            }
        }

        is List<*> -> value.map {
            when (it) {
                is Map<*, *> -> transformMemberNodes(it as Map<String, Any>)
                else -> it
            }
        }

        else -> value
    }
}

private fun MoshiObject.toList(prefix: String = ""): List<Pair<String, String>> = attributes.map { (key, node) ->
    when (node) {
        is MoshiString -> listOf(prefix + key to node.value)
        is MoshiObject -> node.toList()
        is MoshiArray -> node.elements.mapIndexed { index, element ->
            "$key.member.$index" to when (element) {
                is MoshiArray -> error("nested arrays not supported")
                is MoshiBoolean -> element.value.toString()
                is MoshiDecimal -> element.value.toString()
                is MoshiInteger -> element.value.toString()
                is MoshiObject -> error("nested objects not supported")
                is MoshiString -> element.value
                MoshiNull -> ""
            }
        }

        is MoshiBoolean -> listOf(prefix + key to node.value.toString())
        is MoshiDecimal -> listOf(prefix + key to node.value.toString())
        is MoshiInteger -> listOf(prefix + key to node.value.toString())
        MoshiNull -> emptyList()
    }
}.flatten()
