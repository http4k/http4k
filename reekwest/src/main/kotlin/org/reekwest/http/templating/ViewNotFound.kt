package org.reekwest.http.templating

data class ViewNotFound(val view: View) : Exception("Template ${view.template} not found")