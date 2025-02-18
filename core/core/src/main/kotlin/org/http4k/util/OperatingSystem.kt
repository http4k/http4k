package org.http4k.util

enum class OperatingSystem {
    MacOS, Windows, Linux;

    companion object {
        fun detect() = with(System.getProperty("os.name")) {
            when {
                startsWith("Mac OS") -> MacOS
                startsWith("Windows") -> Windows
                else -> Linux
            }
        }
    }
}
