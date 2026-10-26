package org.http4k.webdriver.datastar

class UnsupportedDatastarExpression(val attribute: String, val expression: String, cause: Throwable? = null) :
    RuntimeException("unsupported datastar expression in $attribute: $expression", cause)
