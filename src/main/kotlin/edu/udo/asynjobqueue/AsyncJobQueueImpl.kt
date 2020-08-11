package edu.udo.asynjobqueue

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.Semaphore

internal class AsyncJobQueueImpl(private val executor: ExecutorService) : AsyncJobQueue {
    private val queue = mutableListOf<Job>()
    private val mutex = Semaphore(1)
    private var jobExecuting = false

    override fun submit(job: Runnable): Future<Any?> {
        mutex.acquire()
        try {
            val jobInstance = Job(job)
            if (!jobExecuting) {
                submitForExecution(jobInstance)
            } else {
                queue.add(jobInstance)
            }
            return jobInstance.future
        } finally {
            mutex.release()
        }
    }

    private fun submitForExecution(job: Job) {
        jobExecuting = true
        val future = executor.submit {
            try {
                job.runnable.run()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                submitNextForExecution()
            }
        }
        job.future.wrapped = future
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