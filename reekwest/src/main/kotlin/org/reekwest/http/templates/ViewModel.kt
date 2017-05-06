package org.reekwest.http.templates

import org.reekwest.http.core.ContentType
import org.reekwest.http.lens.BiDiBodyLens
import org.reekwest.http.lens.BiDiBodySpec
import org.reekwest.http.lens.Body
import java.nio.ByteBuffer

interface ViewModel {
    /**
     * This is the path of the template file - which matches the filly qualified classname. The templating suffix
     * is added by the template implementation (eg. java.lang.String -> java/lang/String.hbs)
     */
    fun template(): String = javaClass.name.replace('.', '/')
}

fun Body.view(renderer: TemplateRenderer, contentType: ContentType): BiDiBodyLens<ViewModel> {
    val viewModelBodySpec: BiDiBodySpec<ByteBuffer, ViewModel> = string(contentType).map({ object : ViewModel {} }, renderer::invoke)
    return viewModelBodySpec.required()
}