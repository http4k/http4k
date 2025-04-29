package server.extensive

import dev.forkhandles.result4k.Result4k

interface FooBarRepo {
    fun doSomething(): Result4k<List<String>, Exception>
}
