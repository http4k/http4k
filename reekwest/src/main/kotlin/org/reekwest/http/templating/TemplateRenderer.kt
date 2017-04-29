package org.reekwest.http.templating

interface TemplateRenderer {
    fun toBody(view: View): String
}