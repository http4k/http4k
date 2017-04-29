package org.reekwest.http.templates

interface TemplateRenderer {
    fun toBody(view: View): String
}