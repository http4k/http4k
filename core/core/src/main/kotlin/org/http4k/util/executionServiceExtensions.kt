package org.http4k.util

import org.http4k.filter.ZipkinTracesStorage
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * Decorate the ExecutorService with propagation of Zipkin trace headers.
 *
 * The untimed [ExecutorService.invokeAll] and [ExecutorService.invokeAny] are bounded by
 * [defaultTimeout] so a slow or dead task cannot pin pool threads indefinitely. Callers that
 * pass an explicit timeout keep full control of it.
 */
@JvmOverloads
fun ExecutorService.withRequestTracing(
    storage: ZipkinTracesStorage = ZipkinTracesStorage.THREAD_LOCAL,
    defaultTimeout: Duration = Duration.ofMinutes(1)
) =
    object : ExecutorService by this {
        override fun execute(command: Runnable) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.execute {
                setForCurrentThread(initial)
                command.run()
            }
        }

        override fun <T> submit(task: Callable<T>) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.submit(Callable<T> {
                setForCurrentThread(initial)
                task.call()
            })
        }

        override fun <T> submit(task: Runnable, result: T) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.submit({
                setForCurrentThread(initial)
                task.run()
            }, result)
        }

        override fun submit(task: Runnable) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.submit {
                setForCurrentThread(initial)
                task.run()
            }
        }

        override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>) =
            invokeAll(tasks, defaultTimeout.toMillis(), MILLISECONDS)

        override fun <T> invokeAll(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit
        ) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.invokeAll(tasks.map {
                Callable {
                    setForCurrentThread(initial)
                    it.call()
                }
            }, timeout, unit)
        }

        override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>) =
            invokeAny(tasks, defaultTimeout.toMillis(), MILLISECONDS)

        override fun <T> invokeAny(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit
        ) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.invokeAny(tasks.map {
                Callable {
                    setForCurrentThread(initial)
                    it.call()
                }
            }, timeout, unit)
        }
    }
