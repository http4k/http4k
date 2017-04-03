package org.reekwest.http.core.entity

import org.reekwest.http.core.Response
import java.nio.ByteBuffer

fun Response.entity(value: String) = copy(entity = StringEntity.toEntity(value))

object StringEntity : EntityTransformer<String> {
    override fun fromEntity(entity: Entity?): String = entity?.let { String(it.array()) } ?: ""
    override fun toEntity(value: String): Entity = ByteBuffer.wrap(value.toByteArray())
}
