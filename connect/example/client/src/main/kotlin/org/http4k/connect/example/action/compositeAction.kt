package org.http4k.connect.example.action

import dev.forkhandles.result4k.flatMap
import org.http4k.connect.example.Example
import org.http4k.connect.example.reverse

/**
 * Composite actions to the API can be achieved easily with
 * custom extension functions
 */
fun Example.doubleReverse(input: String) =
    reverse(input).flatMap { reverse(it.value) }
