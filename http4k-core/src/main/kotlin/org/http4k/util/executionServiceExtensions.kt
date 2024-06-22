package org.http4k.util

import org.http4k.filter.ZipkinTracesStorage
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

/**
 * Decorate the ExecutorService with propagation of Zipkin trace headers
 */
fun ExecutorService.withRequestTracing(storage: ZipkinTracesStorage = ZipkinTracesStorage.THREAD_LOCAL) =
    object : ExecutorService by this {
        override fun execute(command: Runnable) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.execute {
                setForCurrentThread(initial)
                command.run()
            }
        }

        override fun <T : Any?> submit(task: Callable<T>) = with(storage) {
            val initial = forCurrentThread()
            this@withRequestTracing.submit(Callable<T> {
                setForCurrentThread(initial)
                task.call()
            })
        }

        override fun <T : Any?> submit(task: Runnable, result: T) = with(storage) {
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

        override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>) =
            with(storage) {
                val initial = forCurrentThread()
                this@withRequestTracing.invokeAll(tasks.map {
                    Callable {
                        setForCurrentThread(initial)
                        it.call()
                    }
                })
            }

        override fun <T : Any?> invokeAll(
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
            })
        }

        override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>) =
            with(storage) {
                val initial = forCurrentThread()
                this@withRequestTracing.invokeAny(tasks.map {
                    Callable {
                        setForCurrentThread(initial)
                        it.call()
                    }
                })
            }

        override fun <T : Any?> invokeAny(
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
            })
        }
    }
