package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private var future: Future<*>? = null

    override fun submit(job: Runnable) {
        if (future?.isDone != false) {
            future = executor.submit(job)
        }
    }
}