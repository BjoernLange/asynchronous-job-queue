package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private val queue = mutableListOf<Runnable>()
    private val mutex = Semaphore(1)
    private var jobExecuting = false

    override fun submit(job: Runnable) {
        mutex.acquire()
        try {
            if (!jobExecuting) {
                submitForExecution(job)
            } else {
                queue.add(job)
            }
        } finally {
            mutex.release()
        }
    }

    private fun submitForExecution(job: Runnable) {
        jobExecuting = true
        executor.submit {
            job.run()
            submitNextForExecution()
        }
    }

    private fun submitNextForExecution() {
        mutex.acquire()
        try {
            if (queue.isNotEmpty()) {
                submitForExecution(queue.removeAt(0))
            } else {
                jobExecuting = false
            }
        } finally {
            mutex.release()
        }
    }
}