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
            val traces = forCurrentThread()
            this@withRequestTracing.execute {
                setForCurrentThread(traces)
                command.run()
            }
        }

        override fun <T : Any?> submit(task: Callable<T>) = with(storage) {
            val traces = forCurrentThread()
            this@withRequestTracing.submit(Callable<T> {
                setForCurrentThread(traces)
                task.call()
            })
        }

        override fun <T : Any?> submit(task: Runnable, result: T) = with(storage) {
            val traces = forCurrentThread()
            this@withRequestTracing.submit({
                setForCurrentThread(traces)
                task.run()
            }, result)
        }

        override fun submit(task: Runnable) = with(storage) {
            val traces = forCurrentThread()
            this@withRequestTracing.submit {
                setForCurrentThread(traces)
                task.run()
            }
        }

        override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>) =
            with(storage) {
                val traces = forCurrentThread()
                this@withRequestTracing.invokeAll(tasks.map {
                    Callable {
                        setForCurrentThread(traces)
                        it.call()
                    }
                })
            }

        override fun <T : Any?> invokeAll(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit
        ) = with(storage) {
            val traces = forCurrentThread()
            this@withRequestTracing.invokeAll(tasks.map {
                Callable {
                    setForCurrentThread(traces)
                    it.call()
                }
            })
        }

        override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>) =
            with(storage) {
                val traces = forCurrentThread()
                this@withRequestTracing.invokeAny(tasks.map {
                    Callable {
                        setForCurrentThread(traces)
                        it.call()
                    }
                })
            }

        override fun <T : Any?> invokeAny(
            tasks: MutableCollection<out Callable<T>>,
            timeout: Long,
            unit: TimeUnit
        ) = with(storage) {
            val traces = forCurrentThread()
            this@withRequestTracing.invokeAny(tasks.map {
                Callable {
                    setForCurrentThread(traces)
                    it.call()
                }
            })
        }
    }
