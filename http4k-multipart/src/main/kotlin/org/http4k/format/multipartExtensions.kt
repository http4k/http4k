package org.http4k.format

import org.http4k.lens.MultipartFormField
import org.http4k.lens.ParamMeta.ObjectParam

fun <NODE: Any> MultipartFormField.Companion.json(auto: Json<NODE>) = with(auto) {
    string().mapWithNewMeta({ parse(it) }, { compact(it) }, ObjectParam)
}

inline fun <reified T : Any> MultipartFormField.Companion.auto(auto: AutoMarshalling) = with(auto) {
    string().mapWithNewMeta({ asA<T>(it) }, { asFormatString(it) }, ObjectParam)
}
