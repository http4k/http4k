package org.reekwest.http.core.entity

import org.reekwest.http.core.Parameters
import org.reekwest.http.core.Request
import org.reekwest.http.core.findSingle
import org.reekwest.http.core.toParameters
import org.reekwest.http.core.toUrlEncoded
import java.nio.ByteBuffer

typealias Form = Parameters

fun Request.form(s: String): String? = extract(FormEntity).findSingle(s)

fun Request.entity(form: Form) = copy(entity = FormEntity.toEntity(form))

fun Form.toEntity() = FormEntity.toEntity(this)

object FormEntity : EntityTransformer<Form> {
    override fun fromEntity(entity: Entity?): Form = entity?.let { String(it.array()) }?.toParameters() ?: emptyList()
    override fun toEntity(value: Form): Entity = ByteBuffer.wrap(value.toUrlEncoded().toByteArray())
}