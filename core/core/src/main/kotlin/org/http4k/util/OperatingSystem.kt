package org.http4k.util

enum class OperatingSystem {
    MacOS, Windows, Linux;

    companion object {
        /**
         * Detect the Host OS from the system properties
         */
        fun detect() = with(System.getProperty("os.name")) {
            when {
                startsWith("Mac OS") -> MacOS
                startsWith("Windows") -> Windows
                else -> Linux
            }
        }
    }
}
