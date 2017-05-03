package org.reekwest.http.templates

import org.reekwest.http.lens.BiDiBodyLens
import org.reekwest.http.lens.BiDiBodySpec
import org.reekwest.http.lens.Body
import java.nio.ByteBuffer

interface ViewModel {
    fun template(): String = javaClass.name.replace('.', '/')
}

fun Body.view(renderer: TemplateRenderer): BiDiBodyLens<ViewModel> {
    val viewModelBodySpec: BiDiBodySpec<ByteBuffer, ViewModel> = string.map({ object : ViewModel {} }, renderer::invoke)
    return viewModelBodySpec.required()
}