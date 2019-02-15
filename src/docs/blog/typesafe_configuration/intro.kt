package blog.typesafe_configuration

data class Port(val value: Int) {
    init {
        if (value < 0 || value > 65535) throw IllegalArgumentException("Out of range Port: $value'")
    }
}

fun main() {
    val port = Port(System.getenv("HTTP_PORT").toInt())
}