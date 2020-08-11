package edu.udo.asynjobqueue

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

internal class FutureWrapper(var wrapped: Future<*>? = null) : Future<Any> {
    override fun isDone(): Boolean {
        return wrapped?.isDone == true
    }

    override fun get(): Any {
        TODO("Not yet implemented")
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