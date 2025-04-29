package server.extensive

import dev.forkhandles.result4k.Result4k

class Hub(private val fooBar: FooBarRepo, private val service: RemoteService) {
    fun doSomethingToRepo(): Result4k<List<String>, Exception> = fooBar.doSomething()
    fun doSomethingWithService(): Result4k<Int, Exception> = service.doSomething()
}
