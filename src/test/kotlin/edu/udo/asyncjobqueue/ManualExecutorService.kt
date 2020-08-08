package edu.udo.asyncjobqueue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.fail
import java.util.concurrent.*

data class Submitted(val runnable: Runnable, val future: CompletableFuture<Void?>)

class ManualExecutorService : ExecutorService {
    val submitted = mutableListOf<Submitted>()

    override fun submit(runnable: Runnable): Future<*> {
        val future = CompletableFuture<Void?>()
        submitted.add(Submitted(runnable = runnable, future = future))
        return future
    }

    fun runNext() {
        assertThat(submitted).isNotEmpty
        val submittedJob = submitted[0]
        submitted.removeAt(0)
        submittedJob.runnable.run()
        submittedJob.future.complete(null)
    }

    override fun shutdown() {
        fail("Should not be called")
    }

    override fun <T : Any?> submit(p0: Callable<T>): Future<T> {
        fail("Should not be called")
    }

    override fun <T : Any?> submit(p0: Runnable, p1: T): Future<T> {
        fail("Should not be called")
    }

    override fun shutdownNow(): MutableList<Runnable> {
        fail("Should not be called")
    }

    override fun isShutdown(): Boolean {
        fail("Should not be called")
    }

    override fun awaitTermination(p0: Long, p1: TimeUnit): Boolean {
        fail("Should not be called")
    }

    override fun <T : Any?> invokeAny(p0: MutableCollection<out Callable<T>>): T {
        fail("Should not be called")
    }

    override fun <T : Any?> invokeAny(p0: MutableCollection<out Callable<T>>, p1: Long, p2: TimeUnit): T {
        fail("Should not be called")
    }

    override fun isTerminated(): Boolean {
        fail("Should not be called")
    }

    override fun <T : Any?> invokeAll(p0: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        fail("Should not be called")
    }

    override fun <T : Any?> invokeAll(p0: MutableCollection<out Callable<T>>, p1: Long, p2: TimeUnit): MutableList<Future<T>> {
        fail("Should not be called")
    }

    override fun execute(p0: Runnable) {
        fail("Should not be called")
    }
}