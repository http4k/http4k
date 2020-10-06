package tutorials.typesafe_configuration.pre

class Port(val value: Int) {
    init {
        require((1..65535).contains(value)) { "Out of range Port: '$value'" }
    }
}

// export PORT=8000
val port = Port(System.getenv("PORT").toInt())
