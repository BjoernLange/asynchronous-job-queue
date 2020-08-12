package edu.udo.asynjobqueue

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class FutureWrapper(var wrapped: Future<Future<*>>) : Future<Any?> {
    override fun isDone() = wrapped.isDone && wrapped.get().isDone

    override fun get(): Any? = wrapped.get().get()

    override fun get(timeout: Long, unit: TimeUnit): Any? {
        val timeoutInMillis = unit.toMillis(timeout)
        val (future, consumedMillis) = measureTimeMillisWithResult {
            wrapped.get(timeoutInMillis, TimeUnit.MILLISECONDS)
        }
        return future.get(timeoutInMillis - consumedMillis, TimeUnit.MILLISECONDS)
    }

    private fun <T> measureTimeMillisWithResult(block: () -> T): Pair<T, Long> {
        val start = System.currentTimeMillis()
        val result = block()
        return result to (System.currentTimeMillis() - start)
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return wrapped.cancel(mayInterruptIfRunning)
    }

    override fun isCancelled(): Boolean {
        TODO("Not yet implemented")
    }
}