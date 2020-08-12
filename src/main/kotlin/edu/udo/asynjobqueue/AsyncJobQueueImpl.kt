package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Semaphore

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private val queue = mutableListOf<Job>()
    private val stateMutex = Semaphore(1)
    private var jobExecuting = false

    override fun submit(job: Runnable): Future<Any?> {
        stateMutex.acquire()
        try {
            val jobInstance = Job(job)
            if (!jobExecuting) {
                submitForExecution(jobInstance)
            } else {
                queue.add(jobInstance)
            }
            return FutureWrapper(jobInstance.future)
        } finally {
            stateMutex.release()
        }
    }

    private fun submitForExecution(job: Job) {
        val cancelSemaphore = Semaphore(0)
        try {
            jobExecuting = true
            val future = executor.submit {
                try {
                    cancelSemaphore.acquire()
                    if (!job.future.isCancelled) {
                        job.runnable.run()
                    }
                } catch (e: InterruptedException) {
                    // May happen when the submitted job is cancelled.
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    submitNextForExecution()
                }
            }
            job.future.complete(future)
        } finally {
            cancelSemaphore.release()
        }
    }

    private fun submitNextForExecution() {
        stateMutex.acquire()
        try {
            if (queue.isNotEmpty()) {
                submitForExecution(queue.removeAt(0))
            } else {
                jobExecuting = false
            }
        } finally {
            stateMutex.release()
        }
    }
}