package org.reekwest.http.core.body

import org.reekwest.http.core.Body
import org.reekwest.http.core.Parameters
import org.reekwest.http.core.Request
import org.reekwest.http.core.findSingle
import org.reekwest.http.core.toParameters
import org.reekwest.http.core.toUrlEncoded
import java.nio.ByteBuffer

typealias Form = Parameters

fun Request.form(s: String): String? = extract(FormBody).findSingle(s)

fun Request.bodyForm(form: Form) = copy(body = FormBody.to(form))

fun Form.toBody(): Body = FormBody.to(this)

fun Request.form(): Form = this.extract(FormBody)

object FormBody : BodyRepresentation<Form> {
    override fun from(body: Body?): Form = body?.let { String(it.array()) }?.toParameters() ?: emptyList()
    override fun to(value: Form): Body = ByteBuffer.wrap(value.toUrlEncoded().toByteArray())
}
