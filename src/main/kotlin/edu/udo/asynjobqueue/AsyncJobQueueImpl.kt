package edu.udo.asynjobqueue

import java.util.concurrent.CompletableFuture
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
        val cancelSemaphore = CompletableFuture<Void?>()
        try {
            jobExecuting = true
            val future = executor.submit {
                try {
                    cancelSemaphore.get()
                    if (!job.future.isCancelled) {
                        job.runnable.run()
                    }
                } finally {
                    submitNextForExecution()
                }
            }
            job.future.complete(future)
        } finally {
            cancelSemaphore.complete(null)
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