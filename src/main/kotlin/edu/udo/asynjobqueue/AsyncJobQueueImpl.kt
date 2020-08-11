package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Semaphore

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private val queue = mutableListOf<Runnable>()
    private val mutex = Semaphore(1)
    private var jobExecuting = false

    override fun submit(job: Runnable): Future<Any> {
        mutex.acquire()
        try {
            return if (!jobExecuting) {
                submitForExecution(job)
            } else {
                queue.add(job)
                FutureWrapper()
            }
        } finally {
            mutex.release()
        }
    }

    private fun submitForExecution(job: Runnable): Future<Any> {
        jobExecuting = true
        return FutureWrapper(executor.submit {
            try {
                job.run()
            } catch (e: Exception) {
                e.printStackTrace();
            } finally {
                submitNextForExecution()
            }
        })
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