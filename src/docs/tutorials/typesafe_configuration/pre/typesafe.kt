package tutorials.typesafe_configuration.pre

import java.time.Duration

data class Timeout(val value: Duration) {
    init {
        require(!value.isNegative) { "Cannot have negative timeout" }
    }
}

// export TIMEOUT=PT30S
val timeout = Timeout(Duration.parse(System.getenv("TIMEOUT")))
