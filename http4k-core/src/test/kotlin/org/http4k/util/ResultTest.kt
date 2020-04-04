@file:Suppress("ConstantConditionIf")

package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ResultTest {

    private val exception = RuntimeException()
    private val value = "hello"
    private val failure: Result<Exception, String> = Failure(exception)
    private val success: Result<Exception, String> = Success(value)

    @Test
    fun `create result from a block`() {
        assertThat(Result { value }, equalTo(success))
        assertThat(Result { if (true) throw exception else value }, equalTo(failure))
    }

    @Test
    fun `map result`() {
        assertThat(success.map { it + it }, equalTo(Success(value + value) as Result<Exception, String>))
        assertThat(failure.map { it + it }, equalTo(failure))
    }

    @Test
    fun `flatMap result`() {
        assertThat(success.flatMap { Success(it + it) }, equalTo(Success(value + value) as Result<Exception, String>))
        assertThat(failure.flatMap { Success(it + it) }, equalTo(failure))
    }

    @Test
    fun `mapFailure result`() {
        val anotherException = IllegalArgumentException()
        assertThat(success.mapFailure { anotherException }, equalTo(success))
        assertThat(failure.mapFailure { anotherException }, equalTo(Failure(anotherException) as Result<Exception, String>))
    }

    @Test
    fun `flatMapFailure result`() {
        val anotherException = IllegalArgumentException()
        assertThat(success.flatMapFailure { Failure(anotherException) }, equalTo(success))
        assertThat(failure.flatMapFailure { Failure(anotherException) }, equalTo(Failure(anotherException) as Result<Exception, String>))
    }
}
