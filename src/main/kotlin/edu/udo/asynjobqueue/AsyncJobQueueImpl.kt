package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService

internal class AsyncJobQueueImpl(val executor: ExecutorService) : AsyncJobQueue {
    override fun submit(job: Runnable) {
        executor.submit(job)
    }
}