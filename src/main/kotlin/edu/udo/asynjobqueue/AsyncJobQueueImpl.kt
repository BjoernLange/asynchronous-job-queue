package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private var future: Future<*>? = null

    override fun submit(job: Runnable) {
        val future = this.future
        if (future == null) {
            this.future = executor.submit(job)
        } else {
            if (future.isDone) {
                this.future = executor.submit(job)
            }
        }
    }
}