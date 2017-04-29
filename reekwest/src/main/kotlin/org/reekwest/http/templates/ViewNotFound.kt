package org.reekwest.http.templates

data class ViewNotFound(val view: View) : Exception("Template ${view.template()} not found")