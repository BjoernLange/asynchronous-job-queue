package edu.udo.asynjobqueue

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class FutureWrapper(var wrapped: Future<Future<*>>) : Future<Any?> {
    override fun isDone(): Boolean {
        return wrapped.isDone && wrapped.get().isDone
    }

    override fun get(): Any? {
        return wrapped.get().get()
    }

    override fun get(p0: Long, p1: TimeUnit): Any {
        TODO("Not yet implemented")
    }

    override fun cancel(p0: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCancelled(): Boolean {
        TODO("Not yet implemented")
    }
}